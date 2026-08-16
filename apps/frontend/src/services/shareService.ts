import axiosInstance from '../lib/axios';
import type { ApiResponse } from '../types/api.types';
import type { PageResponse } from './fileService';

export interface FileShareResponse {
  shareId: string;
  fileId: string;
  fileName: string;
  sharedBy: {
    userId: number;
    username: string;
    name: string;
  };
  sharedWith: {
    userId: number;
    username: string;
    name: string;
  };
  permission: 'VIEW' | 'DOWNLOAD' | 'EDIT';
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  sharedAt: string;
  expiresAt?: string;
}

export interface CreateFileShareRequest {
  sharedWithUserId: number;
  permission: 'VIEW' | 'DOWNLOAD' | 'EDIT';
  expiresAt?: string;
  message?: string;
}

export interface ShareLinkResponse {
  linkId: string;
  fileId: string;
  fileName: string;
  token: string;
  publicUrl: string;
  permission: 'VIEW' | 'DOWNLOAD';
  password?: boolean;
  expiresAt?: string;
  accessCount: number;
  maxAccessCount?: number;
  createdAt: string;
  createdBy: string;
}

export interface CreateShareLinkRequest {
  permission: 'VIEW' | 'DOWNLOAD';
  password?: string;
  expiresAt?: string;
  maxAccessCount?: number;
}

export const shareService = {
  /**
   * Share a file with another user
   */
  async shareFile(
    fileId: string,
    request: CreateFileShareRequest
  ): Promise<ApiResponse<FileShareResponse>> {
    const response = await axiosInstance.post<ApiResponse<FileShareResponse>>(
      `/shares/files/${fileId}`,
      request
    );
    return response.data;
  },

  /**
   * Get files shared with me
   */
  async getFilesSharedWithMe(
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FileShareResponse>>> {
    const response = await axiosInstance.get<
      ApiResponse<PageResponse<FileShareResponse>>
    >('/shares/received', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Get files I shared
   */
  async getFilesSharedByMe(
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FileShareResponse>>> {
    const response = await axiosInstance.get<
      ApiResponse<PageResponse<FileShareResponse>>
    >('/shares/sent', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Accept a share invitation
   */
  async acceptShare(shareId: string): Promise<ApiResponse<FileShareResponse>> {
    const response = await axiosInstance.post<ApiResponse<FileShareResponse>>(
      `/shares/${shareId}/accept`
    );
    return response.data;
  },

  /**
   * Decline a share invitation
   */
  async declineShare(shareId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.post<ApiResponse<void>>(
      `/shares/${shareId}/decline`
    );
    return response.data;
  },

  /**
   * Revoke a share
   */
  async revokeShare(shareId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/shares/${shareId}`
    );
    return response.data;
  },

  /**
   * Create a public share link
   */
  async createShareLink(
    fileId: string,
    request: CreateShareLinkRequest
  ): Promise<ApiResponse<ShareLinkResponse>> {
    const response = await axiosInstance.post<ApiResponse<ShareLinkResponse>>(
      `/shares/links/${fileId}`,
      request
    );
    return response.data;
  },

  /**
   * Get share links for a file
   */
  async getShareLinksForFile(
    fileId: string
  ): Promise<ApiResponse<ShareLinkResponse[]>> {
    const response = await axiosInstance.get<
      ApiResponse<ShareLinkResponse[]>
    >(`/shares/links/${fileId}`);
    return response.data;
  },

  /**
   * Revoke a share link
   */
  async revokeShareLink(linkId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/shares/links/${linkId}`
    );
    return response.data;
  },

  /**
   * Get public share link info (no auth required)
   */
  async getPublicShareLink(
    token: string
  ): Promise<ApiResponse<ShareLinkResponse>> {
    const response = await axiosInstance.get<ApiResponse<ShareLinkResponse>>(
      `/shares/public/${token}`
    );
    return response.data;
  },
};
