import axiosInstance from '../lib/axios';
import type { ApiResponse } from '../types/api.types';

export interface FileMetadata {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  sha256Hash: string;
  storageKey: string;
  isDuplicate: boolean;
  downloadCount: number;
  uploadedAt: string;
  lastModified: string;
  folderId?: string;
}

export interface FileUploadResponse {
  fileId: string;
  fileName: string;
  fileSize: number;
  uploadedAt: string;
  storageKey: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export const fileService = {
  /**
   * Upload a file
   */
  async uploadFile(
    file: File,
    folderId?: string,
    onUploadProgress?: (progress: number) => void
  ): Promise<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    if (folderId) {
      formData.append('folderId', folderId);
    }

    const response = await axiosInstance.post<ApiResponse<FileUploadResponse>>(
      '/files/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (onUploadProgress && progressEvent.total) {
            const progress = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            );
            onUploadProgress(progress);
          }
        },
      }
    );

    return response.data;
  },

  /**
   * Download a file
   */
  async downloadFile(fileId: string, fileName: string): Promise<void> {
    const response = await axiosInstance.get(`/files/${fileId}/download`, {
      responseType: 'blob',
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  /**
   * Get file metadata
   */
  async getFileMetadata(fileId: string): Promise<ApiResponse<FileMetadata>> {
    const response = await axiosInstance.get<ApiResponse<FileMetadata>>(
      `/files/${fileId}`
    );
    return response.data;
  },

  /**
   * List files in a folder
   */
  async listFiles(
    folderId?: string,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FileMetadata>>> {
    const params: any = { page, size, sort: 'createdAt' };
    if (folderId) {
      params.folderId = folderId;
    }

    const response = await axiosInstance.get<
      ApiResponse<PageResponse<FileMetadata>>
    >('/files', { params });
    return response.data;
  },

  /**
   * Search files by name
   */
  async searchFiles(
    query: string,
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FileMetadata>>> {
    const response = await axiosInstance.get<
      ApiResponse<PageResponse<FileMetadata>>
    >('/files/search', {
      params: { q: query, page, size },
    });
    return response.data;
  },

  /**
   * Delete a file
   */
  async deleteFile(fileId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/files/${fileId}`
    );
    return response.data;
  },
};
