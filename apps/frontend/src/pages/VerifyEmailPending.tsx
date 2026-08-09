import { useState, useRef, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, ArrowLeft, RefreshCw, CheckCircle } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Button } from '../components/ui';
import { motion, AnimatePresence } from 'framer-motion';
import { authService } from '../services/authService';
import { tokenService } from '../services/tokenService';
import { useAuthStore } from '../store/authStore';

// OTP TTL on the backend defaults to 5 minutes. We use that as the cooldown
// so the resend button is disabled while an active OTP exists.
const RESEND_COOLDOWN_SECONDS = 5 * 60;

const verifyEmailSchema = z.object({
  otp: z
    .string()
    .min(1, 'Verification code is required')
    .regex(/^\d{6}$/, 'Verification code must be exactly 6 digits'),
});

type VerifyEmailFormData = z.infer<typeof verifyEmailSchema>;

// ── Cooldown hook ──────────────────────────────────────────────────────────────
function useResendCooldown(initialSeconds: number, autoStart: boolean) {
  const [secondsLeft, setSecondsLeft] = useState(initialSeconds);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const start = useCallback((seconds = initialSeconds) => {
    if (timerRef.current) clearInterval(timerRef.current);
    setSecondsLeft(seconds);
    timerRef.current = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          clearInterval(timerRef.current!);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
  }, [initialSeconds]);

  useEffect(() => {
    // Only auto-start cooldown when arriving from registration (an OTP was just sent).
    // When arriving from login page (no fresh OTP), don't lock the resend button.
    if (autoStart) {
      start();
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const formatted = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;

  return { secondsLeft, formatted, restart: start };
}

const VerifyEmailPending = () => {
  const location = useLocation();
  const navigate = useNavigate();

  // Email passed as route state — from Register (always present) or Login
  // (present when user typed their email; absent when they typed a username)
  const stateEmail: string = (location.state as any)?.email ?? '';
  const stateUsername: string = (location.state as any)?.username ?? '';

  // If we only have a username we need the user to supply their email so we
  // can call the resend/verify endpoints which require an email address.
  const [resolvedEmail, setResolvedEmail] = useState(stateEmail);
  const [emailInput, setEmailInput] = useState('');
  const [emailInputError, setEmailInputError] = useState<string | null>(null);

  const email = resolvedEmail;

  const [isSuccess, setIsSuccess] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  // Individual OTP digit inputs
  const [digits, setDigits] = useState<string[]>(['', '', '', '', '', '']);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const { secondsLeft, formatted: cooldownFormatted, restart: restartCooldown } =
    useResendCooldown(RESEND_COOLDOWN_SECONDS, !!stateEmail);

  const { handleSubmit, setValue, formState: { errors } } = useForm<VerifyEmailFormData>({
    resolver: zodResolver(verifyEmailSchema),
    defaultValues: { otp: '' },
  });

  // Redirect to register only if we have neither email nor username
  useEffect(() => {
    if (!stateEmail && !stateUsername) {
      navigate('/register', { replace: true });
    }
  }, [stateEmail, stateUsername, navigate]);

  // Sync digit array into the hidden form field
  useEffect(() => {
    setValue('otp', digits.join(''));
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

  const onSubmit = async (data: VerifyEmailFormData) => {
    setVerifyError(null);
    setIsVerifying(true);
    try {
      // verifyEmail now returns AuthenticationResponse — store tokens immediately
      const response = await authService.verifyEmail({ email, otp: data.otp });

      tokenService.setTokens(response.accessToken, response.refreshToken);
      useAuthStore.getState().setUser(response.user);

      setIsSuccess(true);
    } catch (err: any) {
      setVerifyError(
        err?.message ?? 'Verification failed. Please check the code and try again.'
      );
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = async () => {
    if (secondsLeft > 0) return; // guard — button should be disabled anyway
    setResendMessage(null);
    setVerifyError(null);
    setIsResending(true);
    try {
      await authService.resendVerification(email);
      setResendMessage('A new code has been sent to your inbox.');
      setDigits(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
      restartCooldown();
    } catch (err: any) {
      // The backend returns 429 with a descriptive message — show it directly
      setVerifyError(
        err?.message ?? 'Failed to resend code. Please wait a moment and try again.'
      );
    } finally {
      setIsResending(false);
    }
  };

  // ── Email collection (only when redirected from login with a username) ─────
  if (!resolvedEmail) {
    return (
      <AuthLayout
        title="Verify Your Email"
        subtitle="Enter the email address linked to your account"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          <div className="flex justify-center">
            <div className="relative">
              <div className="absolute inset-0 bg-primary-500/20 rounded-full blur-xl" />
              <div className="relative bg-primary-500/10 p-4 rounded-full">
                <Mail className="w-12 h-12 text-primary-400" />
              </div>
            </div>
          </div>

          <p className="text-center text-dark-400 text-sm">
            Your account <span className="text-dark-200 font-medium">{stateUsername}</span> is
            pending email verification. Enter your email to continue.
          </p>

          {emailInputError && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400 text-center">
              {emailInputError}
            </div>
          )}

          <div className="space-y-2">
            <label className="block text-sm font-medium text-dark-200">
              Email Address
            </label>
            <input
              type="email"
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              placeholder="you@example.com"
              className="w-full h-11 px-4 rounded-lg border border-dark-600 bg-dark-800 text-dark-100 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500/50 transition-colors"
            />
          </div>

          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            disabled={!emailInput.includes('@')}
            onClick={() => {
              if (!emailInput.includes('@')) {
                setEmailInputError('Please enter a valid email address.');
                return;
              }
              setEmailInputError(null);
              setResolvedEmail(emailInput.trim().toLowerCase());
            }}
          >
            Continue
          </Button>

          <div className="text-center">
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="inline-flex items-center gap-2 text-sm text-dark-400 hover:text-primary-400 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to login
            </button>
          </div>
        </motion.div>
      </AuthLayout>
    );
  }

  // ── Success state ──────────────────────────────────────────────────────────
  if (isSuccess) {
    return (
      <AuthLayout title="Email Verified!" subtitle="Your account is now active">
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
            className="text-center space-y-2"
          >
            <p className="text-dark-200">Welcome to Ziboto!</p>
            <p className="text-dark-400 text-sm">
              Your email has been verified. You can now access your account.
            </p>
          </motion.div>

          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => navigate('/initializing', { replace: true })}
          >
            Continue to App
          </Button>
        </motion.div>
      </AuthLayout>
    );
  }

  // ── OTP entry form ─────────────────────────────────────────────────────────
  return (
    <AuthLayout
      title="Verify Your Email"
      subtitle={`We sent a 6-digit code to ${email}`}
    >
      <AnimatePresence mode="wait">
        <motion.div
          key="form"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Email icon */}
          <div className="flex justify-center">
            <div className="relative">
              <div className="absolute inset-0 bg-primary-500/20 rounded-full blur-xl" />
              <div className="relative bg-primary-500/10 p-4 rounded-full">
                <Mail className="w-12 h-12 text-primary-400" />
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Error */}
            {verifyError && (
              <motion.div
                initial={{ opacity: 0, y: -8 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400 text-center"
              >
                {verifyError}
              </motion.div>
            )}

            {/* Resend success */}
            {resendMessage && (
              <motion.div
                initial={{ opacity: 0, y: -8 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-green-500/10 border border-green-500/30 rounded-lg p-3 text-sm text-green-400 text-center"
              >
                {resendMessage}
              </motion.div>
            )}

            {/* OTP digit inputs */}
            <div className="flex justify-center gap-2" onPaste={handleDigitPaste}>
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
                  disabled={isVerifying}
                  aria-label={`Digit ${i + 1} of verification code`}
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
              <p className="text-sm text-red-400 text-center">{errors.otp.message}</p>
            )}

            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              isLoading={isVerifying}
              disabled={isVerifying || digits.join('').length < 6}
            >
              {isVerifying ? 'Verifying...' : 'Verify Email'}
            </Button>
          </form>

          {/* Resend section */}
          <div className="text-center space-y-2">
            <p className="text-sm text-dark-400">Didn't receive the code?</p>

            {secondsLeft > 0 ? (
              <div className="flex items-center justify-center gap-2 text-sm text-dark-500">
                <RefreshCw className="w-4 h-4" />
                <span>
                  Resend available in{' '}
                  <span className="font-mono text-dark-300">{cooldownFormatted}</span>
                </span>
              </div>
            ) : (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                isLoading={isResending}
                disabled={isResending}
                onClick={handleResend}
              >
                <RefreshCw className={`w-4 h-4 mr-2 ${isResending ? 'animate-spin' : ''}`} />
                {isResending ? 'Sending...' : 'Resend code'}
              </Button>
            )}
          </div>

          {/* Back to register */}
          <div className="text-center">
            <button
              type="button"
              onClick={() => navigate('/register')}
              className="inline-flex items-center gap-2 text-sm text-dark-400 hover:text-primary-400 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to register
            </button>
          </div>
        </motion.div>
      </AnimatePresence>
    </AuthLayout>
  );
};

export default VerifyEmailPending;
