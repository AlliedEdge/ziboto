import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import {
  Trash2,
  RotateCcw,
  AlertTriangle,
  Clock,
  FileText,
  Folder,
  ArrowLeft,
  LogOut,
  User,
} from 'lucide-react';
import { Button, Card } from '../components/ui';
import { useAuthStore } from '../store/authStore';
import { trashService, type TrashItemResponse, type TrashSummaryResponse } from '../services/trashService';

const TrashBin = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [trashItems, setTrashItems] = useState<TrashItemResponse[]>([]);
  const [summary, setSummary] = useState<TrashSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  useEffect(() => {
    loadTrash();
  }, []);

  const loadTrash = async () => {
    try {
      setLoading(true);
      const [itemsResponse, summaryResponse] = await Promise.all([
        trashService.getTrash(),
        trashService.getTrashSummary(),
      ]);

      if (itemsResponse.data) {
        setTrashItems(itemsResponse.data.content || []);
      }

      if (summaryResponse.data) {
        setSummary(summaryResponse.data);
      }
    } catch (error) {
      console.error('Failed to load trash:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRestore = async (item: TrashItemResponse) => {
    try {
      if (item.type === 'FILE') {
        await trashService.restoreFile(item.id);
      } else {
        await trashService.restoreFolder(item.id);
      }
      await loadTrash();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to restore item');
    }
  };

  const handlePermanentDelete = async (item: TrashItemResponse) => {
    if (!confirm(`Permanently delete "${item.name}"? This cannot be undone.`)) {
      return;
    }

    try {
      if (item.type === 'FILE') {
        await trashService.permanentlyDeleteFile(item.id);
      }
      await loadTrash();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to delete item');
    }
  };

  const handleEmptyTrash = async () => {
    if (
      !confirm(
        'Empty entire trash? All items will be permanently deleted. This cannot be undone.'
      )
    ) {
      return;
    }

    try {
      await trashService.emptyTrash();
      await loadTrash();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to empty trash');
    }
  };

  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const getIcon = (type: string) => {
    return type === 'FILE' ? (
      <FileText className="w-5 h-5 text-red-400" />
    ) : (
      <Folder className="w-5 h-5 text-yellow-400" />
    );
  };

  return (
    <div className="min-h-screen w-full flex flex-col relative overflow-hidden bg-dark-950">
      {/* Animated background gradients */}
      <div className="absolute inset-0 -z-10">
        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            rotate: [0, 90, 0],
          }}
          transition={{
            duration: 20,
            repeat: Infinity,
            ease: 'linear',
          }}
          className="absolute top-0 -left-20 w-96 h-96 bg-red-600/20 rounded-full blur-3xl"
        />
        <motion.div
          animate={{
            scale: [1, 1.3, 1],
            rotate: [0, -90, 0],
          }}
          transition={{
            duration: 25,
            repeat: Infinity,
            ease: 'linear',
          }}
          className="absolute bottom-0 -right-20 w-96 h-96 bg-red-700/10 rounded-full blur-3xl"
        />
      </div>

      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full border-b border-dark-800/50 backdrop-blur-sm bg-dark-900/50"
      >
        <div className="max-w-7xl w-full mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary-500/20 flex items-center justify-center">
                <User className="w-5 h-5 text-primary-400" />
              </div>
              <div>
                <h2 className="text-white font-medium">{user?.name || 'User'}</h2>
                <p className="text-dark-400 text-sm">{user?.email}</p>
              </div>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleLogout}
              className="flex items-center gap-2"
            >
              <LogOut className="w-4 h-4" />
              Logout
            </Button>
          </div>
        </div>
      </motion.div>

      {/* Main Content */}
      <div className="flex-1 p-6">
        <div className="max-w-7xl w-full mx-auto space-y-6">
          {/* Header */}
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex items-center justify-between"
          >
            <div>
              <h1 className="text-4xl font-bold text-white flex items-center gap-3">
                <Trash2 className="w-10 h-10 text-red-400" />
                Trash Bin
              </h1>
              <p className="text-dark-300 mt-1">
                Items are kept for 30 days before permanent deletion
              </p>
            </div>

            <Button
              variant="secondary"
              size="md"
              onClick={() => navigate('/files')}
              className="flex items-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Files
            </Button>
          </motion.div>

          {/* Summary Card */}
          {summary && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.1 }}
            >
              <Card className="bg-red-500/10 border-red-500/30">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-lg bg-red-500/20 flex items-center justify-center">
                      <Trash2 className="w-6 h-6 text-red-400" />
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-white">
                        {summary.totalItems} items in trash
                      </h3>
                      <p className="text-dark-400 text-sm">
                        {summary.fileCount} files, {summary.folderCount} folders •{' '}
                        {formatBytes(summary.totalSize)}
                      </p>
                    </div>
                  </div>
                  {summary.totalItems > 0 && (
                    <Button
                      variant="danger"
                      size="md"
                      onClick={handleEmptyTrash}
                      className="flex items-center gap-2"
                    >
                      <Trash2 className="w-4 h-4" />
                      Empty Trash
                    </Button>
                  )}
                </div>
              </Card>
            </motion.div>
          )}

          {/* Warning Card */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
          >
            <Card className="bg-yellow-500/10 border-yellow-500/30">
              <div className="flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 text-yellow-400 flex-shrink-0" />
                <div className="text-sm text-dark-300">
                  <strong className="text-yellow-400">Important:</strong> Items
                  in trash will be automatically deleted after 30 days. Restore
                  them before they're gone forever.
                </div>
              </div>
            </Card>
          </motion.div>

          {/* Trash Items */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-4"
          >
            {loading ? (
              <Card>
                <p className="text-dark-400 text-center py-8">Loading...</p>
              </Card>
            ) : trashItems.length === 0 ? (
              <Card>
                <div className="text-center py-12">
                  <Trash2 className="w-16 h-16 text-dark-600 mx-auto mb-4" />
                  <h3 className="text-xl font-semibold text-white mb-2">
                    Trash is empty
                  </h3>
                  <p className="text-dark-400">
                    Deleted files and folders will appear here
                  </p>
                </div>
              </Card>
            ) : (
              <div className="space-y-2">
                {trashItems.map((item) => (
                  <motion.div
                    key={item.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                  >
                    <Card className="hover:border-red-500/50 transition-colors group">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-dark-800 flex items-center justify-center flex-shrink-0">
                          {getIcon(item.type)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <h4 className="text-white font-medium truncate">
                            {item.name}
                          </h4>
                          <div className="flex items-center gap-3 text-xs text-dark-400 mt-1">
                            <span>{item.originalPath}</span>
                            {item.size && <span>{formatBytes(item.size)}</span>}
                          </div>
                          <div className="flex items-center gap-2 mt-1">
                            <Clock className="w-3 h-3 text-dark-500" />
                            <span className="text-xs text-dark-500">
                              Deleted {formatDate(item.deletedAt)} •{' '}
                              {item.daysUntilPermanentDeletion} days left
                            </span>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleRestore(item)}
                            className="flex items-center gap-2"
                          >
                            <RotateCcw className="w-4 h-4" />
                            Restore
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => handlePermanentDelete(item)}
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </div>
                      </div>
                    </Card>
                  </motion.div>
                ))}
              </div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
};

export default TrashBin;
