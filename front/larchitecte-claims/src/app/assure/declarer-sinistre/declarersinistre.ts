import { Component, signal, computed, OnInit, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ClaimService, ClaimRequest } from '../../core/claim.service';
import { AuthService } from '../../core/auth.service';
import { FileService, UploadProgress } from '../../core/file.service';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';

export interface UploadedFile {
  file: File;
  originalName: string;
  storedName: string;
  size: number;
  progress: number;
  uploading: boolean;
  completed: boolean;
  error: string;
  previewUrl: string | null;
}

@Component({
  selector: 'app-declarer-sinistre',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './declarersinistre.html',
  styleUrl: './declarersinistre.css'
})
export class DeclarerSinistre implements OnInit, AfterViewInit, OnDestroy {
  currentStep = signal(1);
  isSubmitting = signal(false);
  submitSuccess = signal(false);
  submitError = signal('');
  isDragOver = signal(false);

  uploadedFiles = signal<UploadedFile[]>([]);
  autoDetectEnabled = signal(false);

  claimForm: FormGroup;

  private uploadSubscriptions: Map<string, Subscription> = new Map();

  // Carte properties
  @ViewChild('map') mapContainer!: ElementRef;
  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private currentLocation: L.LatLng | null = null;

  typeLabels: Record<string, string> = {
    'water': 'Dégât des Eaux',
    'fire': 'Incendie',
    'theft': 'Vol avec Effraction',
    'auto': 'Sinistre Auto',
    'natural': 'Catastrophe naturelle'
  };

  readonly userName = computed(() => this.authService.userName());
  readonly userEmail = computed(() => this.authService.userEmail());
  readonly userId = computed(() => this.authService.userId());

  readonly validFileCount = computed(() =>
    this.uploadedFiles().filter(f => f.completed && !f.error).length
  );
  readonly uploadingCount = computed(() =>
    this.uploadedFiles().filter(f => f.uploading).length
  );

  constructor(
    private fb: FormBuilder,
    private claimService: ClaimService,
    private authService: AuthService,
    private fileService: FileService,
    private router: Router
  ) {
    this.claimForm = this.fb.group({
      type: ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(10)]],
      dateSinistre: ['', Validators.required],
      heureSinistre: ['', Validators.required],
      lieu: ['', Validators.required],
      notesLieu: [''],
      estimation: [''],
      termsAccepted: [false, Validators.requiredTrue]
    });
  }

  nextStep() {
    if (this.currentStep() < 4) {
      if (this.validateCurrentStep()) {
        this.currentStep.update(v => v + 1);
        this.onStepChange();
      }
    }
  }

  previousStep() {
    if (this.currentStep() > 1) {
      this.currentStep.update(v => v - 1);
      this.onStepChange();
    }
  }

  validateCurrentStep(): boolean {
    const step = this.currentStep();
    switch (step) {
      case 1: {
        const type = this.claimForm.get('type');
        const description = this.claimForm.get('description');
        const dateSinistre = this.claimForm.get('dateSinistre');
        if (type) type.markAsTouched();
        if (description) description.markAsTouched();
        if (dateSinistre) dateSinistre.markAsTouched();
        return !!(type?.valid && description?.valid && dateSinistre?.valid);
      }
      case 2: {
        const files = this.uploadedFiles();
        const validFiles = files.filter(f => f.completed && !f.error);
        const hasUploading = files.some(f => f.uploading);

        if (validFiles.length === 0) {
          if (hasUploading) {
            this.submitError.set('Veuillez attendre la fin de l\'upload des fichiers');
          } else {
            this.submitError.set('Veuillez ajouter au moins un document justificatif');
          }
        } else {
          this.submitError.set('');
        }
        return validFiles.length > 0;
      }
      case 3: {
        const lieu = this.claimForm.get('lieu');
        if (lieu) lieu.markAsTouched();
        return !!(lieu?.valid);
      }
      default:
        return true;
    }
  }

  // --- Drag & Drop ---
  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);
    const files = Array.from(event.dataTransfer?.files || []);
    this.processFiles(files);
  }

  handleFileSelect(event: Event) {
    const target = event.target as HTMLInputElement;
    const files = Array.from(target.files || []);
    this.processFiles(files);
    target.value = '';
  }

  private processFiles(files: File[]) {
    const token = this.authService.getToken();
    if (!token) {
      this.submitError.set('Vous devez être connecté pour uploader des fichiers');
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf'];
    const maxSize = 10 * 1024 * 1024;

    for (const file of files) {
      if (!allowedTypes.includes(file.type)) {
        this.submitError.set(`Format non supporté pour "${file.name}". Utilisez JPG, PNG ou PDF.`);
        continue;
      }
      if (file.size > maxSize) {
        this.submitError.set(`Le fichier "${file.name}" dépasse la limite de 10 Mo.`);
        continue;
      }

      const previewUrl = file.type.startsWith('image/') ? URL.createObjectURL(file) : null;
      const fileEntry: UploadedFile = {
        file,
        originalName: file.name,
        storedName: '',
        size: file.size,
        progress: 0,
        uploading: true,
        completed: false,
        error: '',
        previewUrl
      };

      this.uploadedFiles.update(list => [...list, fileEntry]);
      this.uploadSingleFile(fileEntry);
    }
  }

  private uploadSingleFile(fileEntry: UploadedFile) {
    const key = fileEntry.originalName + '_' + fileEntry.size;
    const sub = this.fileService.uploadFileWithProgress(fileEntry.file).subscribe({
      next: (progress: UploadProgress) => {
        this.uploadedFiles.update(list =>
          list.map(f => (f.originalName === fileEntry.originalName && f.size === fileEntry.size)
            ? { ...f, progress: progress.progress, uploading: !progress.completed, completed: progress.completed, storedName: progress.response?.storedName || f.storedName }
            : f
          )
        );
      },
      error: (err) => {
        console.error('Upload error:', err);
        this.uploadedFiles.update(list =>
          list.map(f => (f.originalName === fileEntry.originalName && f.size === fileEntry.size)
            ? { ...f, uploading: false, completed: false, error: 'Échec de l\'upload' }
            : f
          )
        );
        this.uploadSubscriptions.delete(key);
      },
      complete: () => {
        this.uploadSubscriptions.delete(key);
      }
    });
    this.uploadSubscriptions.set(key, sub);
  }

  retryUpload(index: number) {
    const file = this.uploadedFiles()[index];
    if (!file) return;

    this.uploadedFiles.update(list =>
      list.map((f, i) => i === index ? { ...f, uploading: true, completed: false, error: '', progress: 0 } : f)
    );
    this.uploadSingleFile(file);
  }

  removeFile(index: number) {
    const file = this.uploadedFiles()[index];
    if (!file) return;

    const key = file.originalName + '_' + file.size;
    const sub = this.uploadSubscriptions.get(key);
    if (sub) { sub.unsubscribe(); this.uploadSubscriptions.delete(key); }

    if (file.previewUrl) URL.revokeObjectURL(file.previewUrl);

    if (file.storedName && file.completed) {
      this.fileService.deleteFile(file.storedName).subscribe({
        next: () => console.log('Fichier supprimé du serveur:', file.storedName),
        error: () => console.warn('Impossible de supprimer le fichier du serveur:', file.storedName)
      });
    }

    this.uploadedFiles.update(list => list.filter((_, i) => i !== index));
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Octets';
    const k = 1024;
    const sizes = ['Octets', 'Ko', 'Mo', 'Go'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  getFileIcon(file: UploadedFile): string {
    const ext = file.originalName.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return 'picture_as_pdf';
    if (['jpg', 'jpeg', 'png'].includes(ext || '')) return 'image';
    return 'description';
  }

  getTypeLabel(type: string): string {
    return this.typeLabels[type] || type;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  submitClaim() {
    const terms = this.claimForm.get('termsAccepted');
    if (terms) terms.markAsTouched();

    if (!this.claimForm.valid) {
      this.submitError.set('Veuillez remplir tous les champs obligatoires et accepter les conditions.');
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set('');

    const formValue = this.claimForm.value;
    const request: ClaimRequest = {
      type: formValue.type,
      description: formValue.description,
      dateSinistre: formValue.dateSinistre,
      heureSinistre: formValue.heureSinistre || '',
      lieu: formValue.lieu || '',
      notesLieu: formValue.notesLieu || '',
      estimation: formValue.estimation || '',
      latitude: this.currentLocation?.lat,
      longitude: this.currentLocation?.lng,
      piecesJointes: this.uploadedFiles()
        .filter(f => f.completed && !f.error)
        .map(f => f.storedName)
    };

    this.claimService.createClaim(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error('Erreur soumission sinistre:', err);
        let msg = 'Erreur lors de la soumission';
        if (err?.error) {
          if (typeof err.error === 'string') {
            msg = err.error;
          } else if (err.error.error) {
            msg = err.error.error;
          } else if (typeof err.error === 'object') {
            const validationErrors = Object.values(err.error).join(' · ');
            if (validationErrors) msg = validationErrors;
          }
        } else if (err?.message) {
          msg = err.message;
        }
        if (err?.status === 403) {
          msg = 'Accès refusé : vous n\'avez pas les droits nécessaires pour cette action.';
        }
        this.submitError.set(msg);
      }
    });
  }

  goToMesSinistres() {
    this.router.navigate(['/assure/mes-sinistres']);
  }

  newClaim() {
    this.submitSuccess.set(false);
    this.submitError.set('');
    this.currentStep.set(1);
    this.uploadedFiles.update(list => {
      list.forEach(f => { if (f.previewUrl) URL.revokeObjectURL(f.previewUrl); });
      return [];
    });
    this.uploadSubscriptions.forEach(sub => sub.unsubscribe());
    this.uploadSubscriptions.clear();
    this.claimForm.reset();
  }

  ngOnInit() {}

  ngAfterViewInit() {}

  ngOnDestroy() {
    this.destroyMap();
    this.uploadSubscriptions.forEach(sub => sub.unsubscribe());
    this.uploadSubscriptions.clear();
    this.uploadedFiles().forEach(f => { if (f.previewUrl) URL.revokeObjectURL(f.previewUrl); });
  }

  // --- Carte ---
  private initMap() {
    if (this.map) return;

    const defaultCenter: L.LatLng = L.latLng(46.6034, 1.8883);

    this.map = L.map(this.mapContainer.nativeElement).setView(defaultCenter, 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.addMarker(defaultCenter);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.onMapClick(e);
    });

    this.detectCurrentLocation();
  }

  private addMarker(latlng: L.LatLng) {
    if (this.marker) {
      this.marker.setLatLng(latlng);
    } else {
      const customIcon = L.divIcon({
        html: '<div style="background-color: #1976d2; width: 20px; height: 20px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>',
        iconSize: [20, 20],
        iconAnchor: [10, 10],
        popupAnchor: [0, -10],
        className: 'custom-marker'
      });

      this.marker = L.marker(latlng, { icon: customIcon }).addTo(this.map!);
      this.marker.bindPopup('Localisation du sinistre').openPopup();
    }
    this.currentLocation = latlng;
  }

  private onMapClick(e: L.LeafletMouseEvent) {
    this.addMarker(e.latlng);
    this.reverseGeocode(e.latlng);
  }

  private reverseGeocode(latlng: L.LatLng) {
    fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latlng.lat}&lon=${latlng.lng}&accept-language=fr`)
      .then(response => response.json())
      .then(data => {
        if (data && data.display_name) {
          this.claimForm.get('lieu')?.setValue(data.display_name);
        }
      })
      .catch(error => {
        console.error('Erreur lors de la géocodification inversée:', error);
        this.claimForm.get('lieu')?.setValue(`${latlng.lat.toFixed(6)}, ${latlng.lng.toFixed(6)}`);
      });
  }

  private detectCurrentLocation() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const latlng = L.latLng(position.coords.latitude, position.coords.longitude);
          this.addMarker(latlng);
          this.map?.setView(latlng, 13);
          this.reverseGeocode(latlng);
        },
        (error) => {
          console.warn('Géolocalisation non disponible:', error);
        }
      );
    }
  }

  searchLocation() {
    const searchValue = this.claimForm.get('lieu')?.value;
    if (searchValue && this.map) {
      fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchValue)}&limit=1&accept-language=fr`)
        .then(response => response.json())
        .then(data => {
          if (data && data.length > 0) {
            const result = data[0];
            const latlng = L.latLng(parseFloat(result.lat), parseFloat(result.lon));
            this.addMarker(latlng);
            this.map!.setView(latlng, 15);
            this.claimForm.get('lieu')?.setValue(result.display_name);
          }
        })
        .catch(error => {
          console.error('Erreur lors de la recherche de localisation:', error);
        });
    }
  }

  toggleAutoDetect() {
    this.autoDetectEnabled.update(v => !v);
    if (this.autoDetectEnabled()) {
      this.detectCurrentLocation();
      this.claimForm.get('lieu')?.setValue('Localisation automatique détectée');
    } else {
      this.claimForm.get('lieu')?.setValue('');
    }
  }

  private destroyMap() {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.marker = null;
      this.currentLocation = null;
    }
  }

  onStepChange() {
    if (this.currentStep() === 3 && !this.map) {
      setTimeout(() => {
        this.initMap();
      }, 100);
    }
  }
}
