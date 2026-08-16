import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { tokenService } from '../services/tokenService';

/**
 * OAuth Callback Page
 * 
 * Handles the redirect from OAuth provider (Google) after authentication.
 * Extracts tokens from URL parameters and completes the login process.
 */
export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setUser, checkAuth } = useAuthStore();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      try {
        // Extract tokens from URL parameters
        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');
        const error = searchParams.get('error');
        const errorMessage = searchParams.get('message');

        // Handle error case
        if (error) {
          console.error('OAuth error:', error, errorMessage);
          navigate('/login', {
            state: {
              error: errorMessage || 'Google authentication failed. Please try again.',
            },
          });
          return;
        }

        // Validate tokens
        if (!accessToken || !refreshToken) {
          console.error('Missing tokens in OAuth callback');
          navigate('/login', {
            state: {
              error: 'Authentication failed. No tokens received.',
            },
          });
          return;
        }

        // Store tokens
        tokenService.setTokens(accessToken, refreshToken);

        // Verify authentication and load user profile
        await checkAuth();

        // Redirect to dashboard
        navigate('/', { replace: true });
      } catch (error) {
        console.error('Error handling OAuth callback:', error);
        navigate('/login', {
          state: {
            error: 'Authentication failed. Please try again.',
          },
        });
      }
    };

    handleOAuthCallback();
  }, [searchParams, navigate, setUser, checkAuth]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
        <p className="text-gray-600">Completing Google sign-in...</p>
      </div>
    </div>
  );
}
