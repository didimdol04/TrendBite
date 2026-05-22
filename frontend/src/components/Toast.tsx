'use client';
import { Toaster, toast as _toast } from 'react-hot-toast';

export const TbToaster = () => (
  <Toaster
    position="top-right"
    gutter={8}
    toastOptions={{
      duration: 3500,
      style: {
        fontFamily: 'var(--tb-font)',
        fontSize: '13px',
        color: 'var(--tb-ink)',
        background: '#fff',
        border: '1px solid var(--tb-line)',
        borderRadius: 'var(--tb-radius)',
        boxShadow: '0 4px 16px rgba(26,42,108,0.08)',
        padding: '10px 14px',
        maxWidth: '360px',
      },
      success: {
        iconTheme: { primary: 'var(--tb-success)', secondary: '#fff' },
      },
      error: {
        iconTheme: { primary: 'var(--tb-danger)', secondary: '#fff' },
      },
    }}
  />
);

export const toast = {
  success: (msg: string) => _toast.success(msg),
  error: (msg: string) => _toast.error(msg),
  info: (msg: string) => _toast(msg, { icon: 'ℹ️' }),
  loading: (msg: string) => _toast.loading(msg),
  dismiss: (id?: string) => _toast.dismiss(id),
};
