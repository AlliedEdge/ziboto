/**
 * API Type Definitions
 * DTOs and interfaces for Spring Boot backend integration
 */

// ============================================================================
// Request DTOs
// ============================================================================

export interface LoginRequestDto {
  usernameOrEmail: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequestDto {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface RefreshTokenRequestDto {
  refreshToken: string;
}

export interface ForgotPasswordRequestDto {
  email: string;
}

export interface ResetPasswordRequestDto {
  email: string;
  otp: string;
  newPassword: string;
}

export interface VerifyEmailRequestDto {
  email: string;
  otp: string;
}

export interface ResendVerificationRequestDto {
  email: string;
}

export interface SendVerificationEmailRequestDto {
  email: string;
}

// ============================================================================
// Response DTOs
// ============================================================================

export interface UserDto {
  id: string;
  email: string;
  name: string;
  role?: string;
  emailVerified?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponseDto {
  user: UserDto;
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface RefreshTokenResponseDto {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface MessageResponseDto {
  message: string;
  success?: boolean;
  timestamp?: string;
}

export interface UserProfileResponseDto extends UserDto {
  phoneNumber?: string;
  avatarUrl?: string;
  bio?: string;
  preferences?: Record<string, any>;
}

// ============================================================================
// Error Response DTOs
// ============================================================================

export interface ApiErrorResponse {
  message: string;
  error?: string;
  errorCode?: string;
  code?: string;
  statusCode: number;
  timestamp?: string;
  path?: string;
  details?: Record<string, any>;
  errors?: Record<string, string>;
  validationErrors?: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}

// ============================================================================
// API State Types
// ============================================================================

export interface ApiRequestState {
  isLoading: boolean;
  error: string | null;
  data: any | null;
}

export interface ApiError {
  message: string;
  statusCode?: number;
  code?: string;
  details?: Record<string, any>;
  fieldErrors?: Record<string, string>;
  validationErrors?: ValidationError[];
}

// ============================================================================
// Retry Configuration
// ============================================================================

export interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  retryableStatusCodes: number[];
  shouldRetry?: (error: any) => boolean;
}

export const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxRetries: 3,
  retryDelay: 1000,
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
};

// ============================================================================
// API Response Wrapper
// ============================================================================

export interface ApiResponse<T = any> {
  data: T;
  status: number;
  message?: string;
  timestamp?: string;
}

// ============================================================================
// Pagination Types
// ============================================================================

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ============================================================================
// File & Folder Types
// ============================================================================

export interface FileMetadata {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  sha256Hash: string;
  storageKey: string;
  folderId?: string;
  isDuplicate: boolean;
  downloadCount: number;
  uploadedAt: string;
  lastModified: string;
  deletedAt?: string;
}

export interface FileUploadResponse {
  fileId: string;
  fileName: string;
  fileSize: number;
  storageKey: string;
  uploadedAt: string;
  isDuplicate: boolean;
}

export interface FolderData {
  folderId: string;
  folderName: string;
  parentFolderId: string | null;
  folderPath: string;
  createdAt: string;
  deletedAt?: string;
}

export interface FolderRequest {
  folderName: string;
  parentFolderId?: string | null;
}

export interface FolderResponse {
  folderId: string;
  folderName: string;
  parentFolderId: string | null;
  folderPath: string;
  createdAt: string;
}

// ============================================================================
// Trash Bin Types
// ============================================================================

export interface TrashItem {
  id: string;
  name: string;
  type: 'FILE' | 'FOLDER';
  size?: number;
  deletedAt: string;
  deletionExpiresAt: string;
  originalPath: string;
}

export interface TrashSummary {
  totalItems: number;
  totalSize: number;
  filesCount: number;
  foldersCount: number;
}

// ============================================================================
// File Sharing Types
// ============================================================================

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
  permission: 'READ' | 'WRITE' | 'ADMIN';
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED';
  sharedAt: string;
  expiresAt?: string;
}

export interface CreateFileShareRequest {
  sharedWithUserId: number;
  permission: 'READ' | 'WRITE';
  expiresAt?: string;
  message?: string;
}

export interface ShareLinkResponse {
  linkId: string;
  fileId: string;
  fileName: string;
  token: string;
  url: string;
  permission: 'READ' | 'DOWNLOAD';
  hasPassword: boolean;
  maxDownloads?: number;
  downloadCount: number;
  expiresAt?: string;
  createdAt: string;
  createdBy: {
    userId: number;
    username: string;
  };
}

export interface CreateShareLinkRequest {
  permission: 'READ' | 'DOWNLOAD';
  password?: string;
  maxDownloads?: number;
  expiresInHours?: number;
}

// ============================================================================
// File Versioning Types
// ============================================================================

export interface FileVersionResponse {
  versionId: string;
  fileId: string;
  versionNumber: number;
  storageKey: string;
  fileSize: number;
  sha256Hash: string;
  changeDescription?: string;
  versionTag?: string;
  createdBy: {
    userId: number;
    username: string;
  };
  createdAt: string;
  isCurrent: boolean;
}

export interface CreateVersionRequest {
  changeDescription?: string;
  versionTag?: string;
}

export interface VersionCompareResponse {
  oldVersion: FileVersionResponse;
  newVersion: FileVersionResponse;
  sizeChange: number;
  hashChanged: boolean;
  differences?: string[];
}

// ============================================================================
// Comments Types
// ============================================================================

export interface CommentResponse {
  commentId: string;
  fileId: string;
  content: string;
  author: {
    userId: number;
    username: string;
    name: string;
  };
  parentCommentId?: string;
  replies?: CommentResponse[];
  createdAt: string;
  updatedAt?: string;
  isEdited: boolean;
}

export interface CommentRequest {
  content: string;
  parentCommentId?: string;
}

// ============================================================================
// Notifications Types
// ============================================================================

export interface NotificationResponse {
  notificationId: string;
  type: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  title: string;
  message: string;
  isRead: boolean;
  isUrgent: boolean;
  relatedEntityType?: 'FILE' | 'FOLDER' | 'SHARE' | 'COMMENT';
  relatedEntityId?: string;
  createdAt: string;
}

export interface NotificationCount {
  unread: number;
  urgent: number;
}

export interface CreateNotificationRequest {
  userId: number;
  type: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  title: string;
  message: string;
  isUrgent?: boolean;
  relatedEntityType?: string;
  relatedEntityId?: string;
}

// ============================================================================
// Activity Logs Types
// ============================================================================

export interface ActivityLogResponse {
  activityId: string;
  action: string;
  entityType: 'FILE' | 'FOLDER' | 'SHARE' | 'USER';
  entityId: string;
  entityName?: string;
  description: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

export interface ActivitySummaryResponse {
  totalActivities: number;
  fileUploads: number;
  fileDownloads: number;
  fileDeletes: number;
  folderCreates: number;
  shares: number;
  period: string;
}

// ============================================================================
// Analytics Types
// ============================================================================

export interface StorageAnalyticsResponse {
  currentUsage: number;
  totalQuota: number;
  usagePercentage: number;
  fileCount: number;
  folderCount: number;
  largestFiles: Array<{
    fileId: string;
    fileName: string;
    fileSize: number;
  }>;
  fileTypeBreakdown: Array<{
    mimeType: string;
    count: number;
    totalSize: number;
  }>;
  storageHistory: Array<{
    date: string;
    usage: number;
  }>;
  growthRate?: number;
}

// ============================================================================
// Gallery Types
// ============================================================================

export interface GalleryResponse {
  galleryId: string;
  title: string;
  description?: string;
  slug: string;
  isPublic: boolean;
  isPasswordProtected: boolean;
  coverFileId?: string;
  fileCount: number;
  createdBy: {
    userId: number;
    username: string;
  };
  createdAt: string;
  updatedAt?: string;
}

export interface GalleryDetailResponse extends GalleryResponse {
  files: Array<{
    fileId: string;
    fileName: string;
    fileSize: number;
    mimeType: string;
    order: number;
    addedAt: string;
  }>;
}

export interface GalleryRequest {
  title: string;
  description?: string;
  slug?: string;
  isPublic: boolean;
  password?: string;
}

export interface AddFileRequest {
  fileId: string;
  order?: number;
}

// ============================================================================
// User Profile Types
// ============================================================================

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
}

export interface StorageInfoResponse {
  usedStorage: number;
  totalStorage: number;
  fileCount: number;
  folderCount: number;
}

export interface UserSessionResponse {
  sessionId: string;
  deviceInfo: string;
  ipAddress: string;
  lastActivity: string;
  isCurrentSession: boolean;
}
