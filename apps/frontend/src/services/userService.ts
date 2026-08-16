import axiosInstance from '../lib/axios';
import type { ApiResponse } from '../types/api.types';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  name: string;
  role?: string;
  createdAt: string;
  lastLoginAt?: string;
  emailVerified: boolean;
}

export interface StorageInfo {
  usedStorage: number;
  totalStorage: number;
  fileCount: number;
  folderCount: number;
  percentage: number;
}

export interface UserSession {
  sessionId: string;
  deviceInfo: string;
  ipAddress: string;
  location?: string;
  createdAt: string;
  lastAccessedAt: string;
  current: boolean;
}

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
}

export const userService = {
  /**
   * Get current user profile
   */
  async getProfile(): Promise<ApiResponse<UserProfile>> {
    const response = await axiosInstance.get<ApiResponse<UserProfile>>(
      '/users/me'
    );
    return response.data;
  },

  /**
   * Update user profile (full update)
   */
  async updateProfile(
    data: UpdateProfileRequest
  ): Promise<ApiResponse<UserProfile>> {
    const response = await axiosInstance.put<ApiResponse<UserProfile>>(
      '/users/profile',
      data
    );
    return response.data;
  },

  /**
   * Update user profile (partial update)
   */
  async patchProfile(
    data: Partial<UpdateProfileRequest>
  ): Promise<ApiResponse<UserProfile>> {
    const response = await axiosInstance.patch<ApiResponse<UserProfile>>(
      '/users/profile',
      data
    );
    return response.data;
  },

  /**
   * Get storage information
   */
  async getStorageInfo(): Promise<ApiResponse<StorageInfo>> {
    const response = await axiosInstance.get<ApiResponse<StorageInfo>>(
      '/users/storage'
    );
    return response.data;
  },

  /**
   * Get user sessions
   */
  async getSessions(): Promise<ApiResponse<UserSession[]>> {
    const response = await axiosInstance.get<ApiResponse<UserSession[]>>(
      '/users/sessions'
    );
    return response.data;
  },

  /**
   * Revoke a session
   */
  async revokeSession(sessionId: string): Promise<ApiResponse<void>> {
    const response = await axiosInstance.delete<ApiResponse<void>>(
      `/users/sessions/${sessionId}`
    );
    return response.data;
  },
};
