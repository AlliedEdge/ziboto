import { useRef, useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Lock, Mail, CheckCircle } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button, PasswordStrengthIndicator } from '../components/ui';
import { motion } from 'framer-motion';
import { useAuthOperations } from '../hooks/useAuthOperations';

const resetPasswordSchema = z
  .object({
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address'),
    otp: z
      .string()
      .min(1, 'Reset code is required')
      .regex(/^\d{6}$/, 'Reset code must be exactly 6 digits'),
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

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const { resetPassword } = useAuthOperations();

  // Pre-fill email from query param if the reset email link includes it
  const emailFromQuery = searchParams.get('email') ?? '';

  // Individual OTP digit inputs
  const [digits, setDigits] = useState<string[]>(['', '', '', '', '', '']);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    mode: 'onChange',
    defaultValues: {
      email: emailFromQuery,
      otp: '',
      password: '',
      confirmPassword: '',
    },
  });

  const password = watch('password');

  // Sync digits array into the hidden otp field
  useEffect(() => {
    setValue('otp', digits.join(''), { shouldValidate: digits.join('').length === 6 });
  }, [digits, setValue]);

  const handleDigitChange = (index: number, value: string) => {
    const digit = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = digit;
    setDigits(next);
    if (digit && index < 5) inputRefs.current[index + 1]?.focus();
  };

  const handleDigitKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (digits[index]) {
        const next = [...digits];
        next[index] = '';
        setDigits(next);
      } else if (index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleDigitPaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const next = ['', '', '', '', '', ''];
    pasted.split('').forEach((ch, i) => { next[i] = ch; });
    setDigits(next);
    inputRefs.current[Math.min(pasted.length, 5)]?.focus();
  };

  const onSubmit = async (data: ResetPasswordFormData) => {
    try {
      await resetPassword.handleExecute({
        email: data.email,
        otp: data.otp,
        newPassword: data.password,
      });
    } catch (error) {
      console.error('Reset password failed:', error);
    }
  };

  // ── Success state ────────────────────────────────────────────────────────
  if (resetPassword.isSuccess) {
    return (
      <AuthLayout
        title="Password Reset Successful"
        subtitle="Your password has been successfully reset"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.1 }}
            className="flex justify-center"
          >
            <div className="relative">
              <div className="absolute inset-0 bg-green-500/20 rounded-full blur-xl" />
              <div className="relative bg-green-500/10 p-4 rounded-full">
                <CheckCircle className="w-16 h-16 text-green-500" />
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="text-center"
          >
            <p className="text-dark-200">You can now sign in with your new password.</p>
          </motion.div>

          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => (window.location.href = '/login')}
          >
            Continue to Login
          </Button>
        </motion.div>
      </AuthLayout>
    );
  }

  // ── Reset form ────────────────────────────────────────────────────────────
  return (
    <AuthLayout
      title="Reset Password"
      subtitle="Enter the code from your email and choose a new password"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* API error */}
        {resetPassword.error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400"
          >
            {resetPassword.error}
          </motion.div>
        )}

        {/* Email */}
        <Input
          {...register('email')}
          type="email"
          label="Email Address"
          placeholder="you@example.com"
          error={errors.email?.message}
          leftIcon={<Mail className="w-5 h-5" />}
          autoComplete="email"
          disabled={resetPassword.isLoading}
        />

        {/* OTP digits */}
        <div className="space-y-2">
          <label className="block text-sm font-medium text-dark-200">
            6-Digit Reset Code
          </label>
          <div className="flex gap-2" onPaste={handleDigitPaste}>
            {digits.map((digit, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleDigitChange(i, e.target.value)}
                onKeyDown={(e) => handleDigitKeyDown(i, e)}
                disabled={resetPassword.isLoading}
                aria-label={`Digit ${i + 1} of reset code`}
                className={[
                  'w-11 h-14 text-center text-xl font-semibold rounded-lg border transition-colors',
                  'bg-dark-800 text-dark-100 outline-none',
                  digit
                    ? 'border-primary-500 ring-1 ring-primary-500/50'
                    : 'border-dark-600 focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50',
                  'disabled:opacity-50 disabled:cursor-not-allowed',
                ].join(' ')}
              />
            ))}
          </div>
          {errors.otp && (
            <p className="text-sm text-red-400">{errors.otp.message}</p>
          )}
        </div>

        {/* New password */}
        <div>
          <Input
            {...register('password')}
            type="password"
            label="New Password"
            placeholder="Create a strong password"
            error={errors.password?.message}
            leftIcon={<Lock className="w-5 h-5" />}
            showPasswordToggle
            autoComplete="new-password"
            disabled={resetPassword.isLoading}
          />
          <PasswordStrengthIndicator password={password} />
        </div>

        {/* Confirm password */}
        <Input
          {...register('confirmPassword')}
          type="password"
          label="Confirm New Password"
          placeholder="Re-enter your password"
          error={errors.confirmPassword?.message}
          leftIcon={<Lock className="w-5 h-5" />}
          showPasswordToggle
          autoComplete="new-password"
          disabled={resetPassword.isLoading}
        />

        {/* Password requirements */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="bg-dark-800/50 border border-dark-700 rounded-lg p-4 space-y-2"
        >
          <p className="text-xs font-medium text-dark-300 mb-2">Password must contain:</p>
          <ul className="space-y-1.5 text-xs text-dark-400">
            {[
              { label: 'At least 8 characters', test: (p: string) => p?.length >= 8 },
              { label: 'One uppercase letter', test: (p: string) => /[A-Z]/.test(p || '') },
              { label: 'One lowercase letter', test: (p: string) => /[a-z]/.test(p || '') },
              { label: 'One number',            test: (p: string) => /[0-9]/.test(p || '') },
            ].map(({ label, test }) => (
              <li key={label} className="flex items-center gap-2">
                <div className={`w-1.5 h-1.5 rounded-full ${test(password) ? 'bg-green-500' : 'bg-dark-600'}`} />
                {label}
              </li>
            ))}
          </ul>
        </motion.div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={resetPassword.isLoading}
          disabled={resetPassword.isLoading}
        >
          {resetPassword.isLoading ? 'Resetting password...' : 'Reset Password'}
        </Button>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-center"
        >
          <Link
            to="/login"
            className="text-sm text-dark-300 hover:text-primary-400 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={resetPassword.isLoading ? -1 : 0}
          >
            Back to login
          </Link>
        </motion.div>
      </form>
    </AuthLayout>
  );
};

export default ResetPassword;
