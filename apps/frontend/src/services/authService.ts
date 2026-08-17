/**
 * Authentication Service
 * Handles all API calls related to authentication with Spring Boot backend
 */

import axios from '../lib/axios';
import { withRetry } from '../utils/retryHandler';
import { logError, normalizeError } from '../utils/apiErrorHandler';
import type {
  LoginRequestDto,
  RegisterRequestDto,
  AuthResponseDto,
  RefreshTokenResponseDto,
  UserProfileResponseDto,
  MessageResponseDto,
  ForgotPasswordRequestDto,
  ResetPasswordRequestDto,
  VerifyEmailRequestDto,
  RetryConfig,
  ApiResponse,
} from '../types/api.types';
import { DEFAULT_RETRY_CONFIG } from '../types/api.types';

// ============================================================================
// Legacy Type Exports (for backward compatibility)
// ============================================================================

export interface LoginCredentials extends LoginRequestDto {}
export interface RegisterData extends RegisterRequestDto {}
export interface User extends UserProfileResponseDto {}
export interface AuthResponse extends AuthResponseDto {}
export interface ForgotPasswordData extends ForgotPasswordRequestDto {}
export interface ResetPasswordData extends ResetPasswordRequestDto {}
export interface RefreshTokenResponse extends RefreshTokenResponseDto {}

// ============================================================================
// Retry Configuration for Auth Requests
// ============================================================================

const AUTH_RETRY_CONFIG: RetryConfig = {
  ...DEFAULT_RETRY_CONFIG,
  maxRetries: 2, // Fewer retries for auth requests
  retryDelay: 1000,
  // Don't retry auth errors (401), validation errors (400), or conflict errors (409)
  retryableStatusCodes: [408, 500, 502, 503, 504],
  shouldRetry: (error: any) => {
    const status = error?.response?.status;
    // Never retry client errors — only transient server/network errors
    if (status >= 400 && status < 500) {
      return false;
    }
    return true;
  },
};

// ============================================================================
// Authentication Service
// ============================================================================

export const authService = {
  /**
   * Login with email and password
   * POST /api/v1/auth/login
   */
  async login(credentials: LoginRequestDto): Promise<AuthResponseDto> {
    try {
      const response = await withRetry(
        async () => {
          return axios.post<ApiResponse<AuthResponseDto>>('/auth/login', {
            usernameOrEmail: credentials.usernameOrEmail,
            password: credentials.password,
          });
        },
        AUTH_RETRY_CONFIG
      );

      // Backend returns ApiResponse<AuthResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.login');
      throw normalizeError(error);
    }
  },

  /**
   * Register new user — returns void (202 Accepted, no tokens)
   * POST /api/v1/auth/register
   */
  async register(data: RegisterRequestDto): Promise<void> {
    try {
      await withRetry(
        async () => {
          return axios.post<ApiResponse<void>>('/auth/register', {
            username: data.username,
            email: data.email,
            password: data.password,
            firstName: data.firstName,
            lastName: data.lastName,
          });
        },
        AUTH_RETRY_CONFIG
      );
      // Backend returns 202 with no token payload — nothing to unwrap
    } catch (error: any) {
      logError(error, 'authService.register');
      throw normalizeError(error);
    }
  },

  /**
   * Logout user
   * POST /api/v1/auth/logout
   */
  async logout(): Promise<void> {
    try {
      await axios.post('/auth/logout');
    } catch (error) {
      // Continue with local logout even if API call fails
      logError(error, 'authService.logout');
      console.warn('Logout API call failed, continuing with local logout');
    }
  },

  /**
   * Refresh access token
   * POST /api/v1/auth/refresh
   */
  async refreshToken(refreshToken: string): Promise<RefreshTokenResponseDto> {
    try {
      const response = await axios.post<ApiResponse<RefreshTokenResponseDto>>('/auth/refresh', {
        refreshToken,
      });

      // Backend returns ApiResponse<RefreshTokenResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.refreshToken');
      throw normalizeError(error);
    }
  },

  /**
   * Get current user profile
   * GET /api/v1/users/me
   */
  async getProfile(): Promise<UserProfileResponseDto> {
    try {
      const response = await withRetry(
        async () => {
          return axios.get<ApiResponse<UserProfileResponseDto>>('/users/me');
        },
        {
          ...AUTH_RETRY_CONFIG,
          maxRetries: 1, // Only retry once for profile requests
        }
      );

      // Backend returns ApiResponse<UserProfileResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.getProfile');
      throw normalizeError(error);
    }
  },

  /**
   * Send forgot password email
   * POST /api/v1/auth/password/forgot
   */
  async forgotPassword(data: ForgotPasswordRequestDto): Promise<MessageResponseDto> {
    try {
      const response = await withRetry(
        async () => {
          return axios.post<ApiResponse<MessageResponseDto>>('/auth/password/forgot', {
            email: data.email,
          });
        },
        AUTH_RETRY_CONFIG
      );

      // Backend returns ApiResponse<MessageResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.forgotPassword', { email: data.email });
      throw normalizeError(error);
    }
  },

  /**
   * Reset password with OTP
   * POST /api/v1/auth/password/reset
   */
  async resetPassword(data: ResetPasswordRequestDto): Promise<MessageResponseDto> {
    try {
      const response = await axios.post<ApiResponse<MessageResponseDto>>('/auth/password/reset', {
        email: data.email,
        otp: data.otp,
        newPassword: data.newPassword,
      });

      // Backend returns ApiResponse<MessageResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.resetPassword');
      throw normalizeError(error);
    }
  },

  /**
   * Verify email with OTP — returns full AuthenticationResponse (account activated)
   * POST /api/v1/auth/email/verify
   */
  async verifyEmail(request: VerifyEmailRequestDto): Promise<AuthResponseDto> {
    try {
      console.log('[authService] Calling verifyEmail API:', { email: request.email, otp: request.otp });
      
      const response = await axios.post<ApiResponse<AuthResponseDto>>('/auth/email/verify', {
        email: request.email,
        otp: request.otp,
      });

      console.log('[authService] verifyEmail response:', response.data);

      // Backend returns ApiResponse<AuthResponseDto> with tokens on success
      if (!response.data.data) {
        console.error('[authService] verifyEmail: response.data.data is missing!', response.data);
        throw new Error('Invalid response structure from server');
      }

      return response.data.data;
    } catch (error: any) {
      console.error('[authService] verifyEmail error:', error);
      logError(error, 'authService.verifyEmail');
      throw normalizeError(error);
    }
  },

  /**
   * Resend verification email
   * POST /api/v1/auth/email/send-verification
   */
  async resendVerification(email: string): Promise<MessageResponseDto> {
    try {
      const response = await withRetry(
        async () => {
          return axios.post<ApiResponse<MessageResponseDto>>('/auth/email/send-verification', {
            email,
          });
        },
        AUTH_RETRY_CONFIG
      );

      // Backend returns ApiResponse<MessageResponseDto>, so we need to unwrap it
      return response.data.data;
    } catch (error: any) {
      logError(error, 'authService.resendVerification', { email });
      throw normalizeError(error);
    }
  },

  /**
   * Check if email is available
   * GET /api/v1/auth/check-email?email=...
   */
  async checkEmailAvailability(email: string): Promise<boolean> {
    try {
      const response = await axios.get<{ available: boolean }>('/auth/check-email', {
        params: { email },
      });

      return response.data.available;
    } catch (error: any) {
      logError(error, 'authService.checkEmailAvailability', { email });
      // If check fails, assume email might be taken
      return false;
    }
  },

  /**
   * Validate token
   * GET /api/v1/auth/validate-token?token=...
   */
  async validateToken(token: string): Promise<boolean> {
    try {
      const response = await axios.get<{ valid: boolean }>('/auth/validate-token', {
        params: { token },
      });

      return response.data.valid;
    } catch (error: any) {
      logError(error, 'authService.validateToken');
      return false;
    }
  },
};

