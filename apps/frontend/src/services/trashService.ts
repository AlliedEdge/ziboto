import axiosInstance from '../lib/axios';
import type { ApiResponse } from '../types/api.types';
import type { PageResponse } from './fileService';

export interface TrashItemResponse {
  id: string;
  name: string;
  type: 'FILE' | 'FOLDER';
  size?: number;
  deletedAt: string;
  deletedBy: string;
  originalPath: string;
  daysUntilPermanentDeletion: number;
}

export interface TrashSummaryResponse {
  totalItems: number;
  totalSize: number;
  fileCount: number;
  folderCount: number;
}

export const trashService = {
  /**
   * Get trash items
   */
  async getTrash(
    page: number = 0,
    size: number = 50
  ): Promise<ApiResponse<PageResponse<TrashItemResponse>>> {
    const response = await axiosInstance.get<
      ApiResponse<PageResponse<TrashItemResponse>>
    >('/trash', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Get trash summary
   */
  async getTrashSummary(): Promise<ApiResponse<TrashSummaryResponse>> {
    const response = await axiosInstance.get<
      ApiResponse<TrashSummaryResponse>
    >('/trash/summary');
    return response.data;
  },

  /**
   * Restore file from trash
   */
  async restoreFile(fileId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.post<ApiResponse<void>>(
      `/trash/files/${fileId}/restore`
    );
    return response.data;
  },

  /**
   * Restore folder from trash
   */
  async restoreFolder(folderId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.post<ApiResponse<void>>(
      `/trash/folders/${folderId}/restore`
    );
    return response.data;
  },

  /**
   * Permanently delete file
   */
  async permanentlyDeleteFile(fileId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/trash/files/${fileId}`
    );
    return response.data;
  },

  /**
   * Empty entire trash
   */
  async emptyTrash(): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      '/trash/empty'
    );
    return response.data;
  },
};
