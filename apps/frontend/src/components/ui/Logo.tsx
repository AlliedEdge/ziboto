import { motion } from 'framer-motion';

interface LogoProps {
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
  className?: string;
}

const Logo: React.FC<LogoProps> = ({ 
  size = 'md', 
  showText = true, 
  className = '' 
}) => {
  const sizes = {
    sm: { container: 'w-6 h-6', text: 'text-xl' },
    md: { container: 'w-10 h-10', text: 'text-3xl' },
    lg: { container: 'w-16 h-16', text: 'text-5xl' },
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className={`flex items-center gap-3 ${className}`}
    >
      <motion.div
        whileHover={{ scale: 1.05 }}
        transition={{ duration: 0.3 }}
        className="relative"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl blur-lg opacity-50" />
        <div className="relative bg-white p-1.5 rounded-xl shadow-glow-sm">
          <img 
            src="/logo.svg" 
            alt="Ziboto" 
            className={`${sizes[size].container} object-contain`}
          />
        </div>
      </motion.div>
      
      {showText && (
        <motion.h1
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1, duration: 0.3 }}
          className={`${sizes[size].text} font-bold text-gradient`}
        >
          Ziboto
        </motion.h1>
      )}
    </motion.div>
  );
};

export default Logo;
