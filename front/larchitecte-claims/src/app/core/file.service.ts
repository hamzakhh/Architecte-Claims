import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UploadProgress {
  progress: number;
  completed: boolean;
  response?: { storedName: string; originalName: string };
}

@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly API_URL = '/api/files';
  // private readonly API_URL = '/api/files';

  constructor(private http: HttpClient) {}

  uploadFileWithProgress(file: File): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', `${this.API_URL}/upload-single`, formData, {
      reportProgress: true,
      responseType: 'json'
    });

    return new Observable<UploadProgress>(observer => {
      this.http.request(req).subscribe({
        next: (event: HttpEvent<any>) => {
          if (event.type === HttpEventType.UploadProgress) {
            const progress = Math.round((100 * event.loaded) / (event.total || 1));
            observer.next({ progress, completed: false });
          } else if (event.type === HttpEventType.Response) {
            observer.next({
              progress: 100,
              completed: true,
              response: event.body as { storedName: string; originalName: string }
            });
            observer.complete();
          }
        },
        error: (err) => observer.error(err)
      });
    });
  }

  deleteFile(storedName: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${storedName}`);
  }

  uploadFiles(files: File[]): Observable<string[]> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return this.http.post<string[]>(`${this.API_URL}/upload`, formData);
  }

  getDownloadUrl(filename: string): string {
    return `${this.API_URL}/download/${filename}`;
  }
}
