import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, Lock, User } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button, PasswordStrengthIndicator } from '../components/ui';
import { motion } from 'framer-motion';
import { useAuthStore } from '../store/authStore';

// Validation schema
const registerSchema = z
  .object({
    username: z
      .string()
      .min(1, 'Username is required')
      .min(3, 'Username must be at least 3 characters')
      .max(50, 'Username must be less than 50 characters')
      .regex(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens'),
    firstName: z
      .string()
      .max(100, 'First name must be less than 100 characters')
      .optional(),
    lastName: z
      .string()
      .max(100, 'Last name must be less than 100 characters')
      .optional(),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address'),
    password: z
      .string()
      .min(1, 'Password is required')
      .min(8, 'Password must be at least 8 characters')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

const Register = () => {
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);
  const error = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  
  const [isLoading, setIsLoading] = useState(false);

  const {
    register: registerField,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onChange',
    defaultValues: {
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    clearError();

    try {
      await register({
        username: data.username,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
      });
      // Navigate to email verification page so the user can confirm their address
      navigate('/verify-email-pending', { replace: true, state: { email: data.email } });
    } catch (error: any) {
      console.error('Registration failed:', error);
      // If the backend says a verification is already in progress for this email,
      // navigate to the verify page anyway — the user just needs to check their inbox.
      const msg: string = error?.message ?? '';
      if (
        error?.status === 409 &&
        msg.toLowerCase().includes('verification email was already sent')
      ) {
        navigate('/verify-email-pending', { replace: true, state: { email: data.email } });
        return;
      }
      // All other errors are handled by the store (displayed in the form)
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSignUp = () => {
    const apiUrl = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8080';
    window.location.href = `${apiUrl}/oauth2/authorization/google`;
  };

  return (
    <AuthLayout
      title="Create Account"
      subtitle="Sign up to get started with Ziboto"
    >
      {/* Google Sign-Up Button */}
      <Button
        type="button"
        variant="secondary"
        size="lg"
        fullWidth
        disabled={isLoading}
        onClick={handleGoogleSignUp}
        className="flex items-center justify-center gap-3"
        id="google-signup-btn"
      >
        <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
          />
          <path
            fill="currentColor"
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
          />
          <path
            fill="currentColor"
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
          />
          <path
            fill="currentColor"
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
          />
        </svg>
        Continue with Google
      </Button>

      {/* Divider */}
      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-dark-600"></div>
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="px-4 bg-dark-800 text-dark-400">Or sign up with email</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Error Message */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400"
          >
            {error}
          </motion.div>
        )}

        {/* Username Input */}
        <Input
          {...registerField('username')}
          type="text"
          label="Username"
          placeholder="johndoe"
          error={errors.username?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="username"
          disabled={isLoading}
        />

        {/* First Name Input */}
        <Input
          {...registerField('firstName')}
          type="text"
          label="First Name (Optional)"
          placeholder="John"
          error={errors.firstName?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="given-name"
          disabled={isLoading}
        />

        {/* Last Name Input */}
        <Input
          {...registerField('lastName')}
          type="text"
          label="Last Name (Optional)"
          placeholder="Doe"
          error={errors.lastName?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="family-name"
          disabled={isLoading}
        />

        {/* Email Input */}
        <Input
          {...registerField('email')}
          type="email"
          label="Email Address"
          placeholder="you@example.com"
          error={errors.email?.message}
          leftIcon={<Mail className="w-5 h-5" />}
          autoComplete="email"
          disabled={isLoading}
        />

        {/* Password Input */}
        <div>
          <Input
            {...registerField('password')}
            type="password"
            label="Password"
            placeholder="Create a strong password"
            error={errors.password?.message}
            leftIcon={<Lock className="w-5 h-5" />}
            showPasswordToggle
            autoComplete="new-password"
            disabled={isLoading}
          />
          <PasswordStrengthIndicator password={password} />
        </div>

        {/* Confirm Password Input */}
        <Input
          {...registerField('confirmPassword')}
          type="password"
          label="Confirm Password"
          placeholder="Re-enter your password"
          error={errors.confirmPassword?.message}
          leftIcon={<Lock className="w-5 h-5" />}
          showPasswordToggle
          autoComplete="new-password"
          disabled={isLoading}
        />

        {/* Terms and Privacy */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="text-xs text-dark-400 text-center"
        >
          By creating an account, you agree to our{' '}
          <Link
            to="/terms"
            className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={isLoading ? -1 : 0}
          >
            Terms of Service
          </Link>{' '}
          and{' '}
          <Link
            to="/privacy"
            className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={isLoading ? -1 : 0}
          >
            Privacy Policy
          </Link>
        </motion.div>

        {/* Submit Button */}
        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Creating account...' : 'Create Account'}
        </Button>

        {/* Sign In Link */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="text-center mt-6"
        >
          <p className="text-sm text-dark-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-primary-400 hover:text-primary-300 font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
              tabIndex={isLoading ? -1 : 0}
            >
              Sign in
            </Link>
          </p>
        </motion.div>
      </form>
    </AuthLayout>
  );
};

export default Register;
