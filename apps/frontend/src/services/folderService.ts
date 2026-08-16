import axiosInstance from '../lib/axios';
import type { ApiResponse } from '../types/api.types';

export interface FolderResponse {
  folderId: string;
  folderName: string;
  parentFolderId: string | null;
  folderPath: string;
  createdAt: string;
  createdBy: string;
  userId: number;
}

export interface FolderRequest {
  folderName: string;
  parentFolderId?: string;
}

export const folderService = {
  /**
   * Create a new folder
   */
  async createFolder(
    request: FolderRequest
  ): Promise<ApiResponse<FolderResponse>> {
    const response = await axiosInstance.post<ApiResponse<FolderResponse>>(
      '/folders',
      request
    );
    return response.data;
  },

  /**
   * Get folder by ID
   */
  async getFolder(folderId: string): Promise<ApiResponse<FolderResponse>> {
    const response = await axiosInstance.get<ApiResponse<FolderResponse>>(
      `/folders/${folderId}`
    );
    return response.data;
  },

  /**
   * List folders in a parent folder
   */
  async listFolders(
    parentFolderId?: string
  ): Promise<ApiResponse<FolderResponse[]>> {
    const params = parentFolderId ? { parentFolderId } : {};
    const response = await axiosInstance.get<ApiResponse<FolderResponse[]>>(
      '/folders',
      { params }
    );
    return response.data;
  },

  /**
   * Rename a folder
   */
  async renameFolder(
    folderId: string,
    newName: string
  ): Promise<ApiResponse<FolderResponse>> {
    const response = await axiosInstance.patch<ApiResponse<FolderResponse>>(
      `/folders/${folderId}/rename`,
      null,
      {
        params: { newName },
      }
    );
    return response.data;
  },

  /**
   * Move a folder to a new parent
   */
  async moveFolder(
    folderId: string,
    newParentId?: string
  ): Promise<ApiResponse<FolderResponse>> {
    const params = newParentId ? { newParentId } : {};
    const response = await axiosInstance.patch<ApiResponse<FolderResponse>>(
      `/folders/${folderId}/move`,
      null,
      { params }
    );
    return response.data;
  },

  /**
   * Delete a folder and all its contents
   */
  async deleteFolder(folderId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/folders/${folderId}`
    );
    return response.data;
  },
};
