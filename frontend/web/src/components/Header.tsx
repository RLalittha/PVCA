import React from 'react';
import { ShoppingBag, Server, CheckCircle2, AlertTriangle } from 'lucide-react';

interface HeaderProps {
  backendOnline: boolean | null;
  backendVersion?: string;
}

export const Header: React.FC<HeaderProps> = ({ backendOnline, backendVersion }) => {
  return (
    <header className="border-b border-slate-800 bg-[#0B132B]/95 backdrop-blur-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#0077B6] to-[#48CAE4] flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <ShoppingBag className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-extrabold text-xl tracking-tight bg-gradient-to-r from-white via-cyan-100 to-cyan-400 bg-clip-text text-transparent">
                PVCA
              </span>
              <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-300 border border-cyan-800/60">
                Web Assistant
              </span>
            </div>
            <p className="text-xs text-slate-400 font-medium hidden sm:block">
              See it. Identify it. Shop it.
            </p>
          </div>
        </div>

        {/* Backend Status Pill */}
        <div className="flex items-center space-x-3 text-xs">
          <div
            className={`flex items-center space-x-1.5 px-3 py-1 rounded-full border ${
              backendOnline === true
                ? 'bg-emerald-950/60 border-emerald-600/50 text-emerald-300'
                : backendOnline === false
                ? 'bg-amber-950/60 border-amber-600/50 text-amber-300'
                : 'bg-slate-900 border-slate-700 text-slate-400'
            }`}
          >
            <Server className="w-3.5 h-3.5" />
            {backendOnline === true ? (
              <>
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                <span className="font-medium">
                  FastAPI Backend Connected {backendVersion ? `(v${backendVersion})` : ''}
                </span>
              </>
            ) : backendOnline === false ? (
              <>
                <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
                <span className="font-medium">Backend Offline (Local Fallback)</span>
              </>
            ) : (
              <span>Connecting to Backend...</span>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
