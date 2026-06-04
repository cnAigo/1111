import { CheckCircle2, XCircle, AlertTriangle, Gauge, X } from 'lucide-react';

const m = { success:'bg-white border-emerald-200 text-emerald-800', error:'bg-white border-red-200 text-red-800', warning:'bg-white border-amber-200 text-amber-800', info:'bg-white border-blue-200 text-blue-800' };
const i = { success:<CheckCircle2 size={15} className="text-emerald-500"/>, error:<XCircle size={15} className="text-red-500"/>, warning:<AlertTriangle size={15} className="text-amber-500"/>, info:<Gauge size={15} className="text-blue-500"/> };

export default function Toast({ toast, onClose }) {
  if (!toast) return null;
  return (
    <div className={`fixed bottom-5 right-5 z-50 flex items-center gap-2.5 px-4 py-3 rounded-xl border shadow-lg animate-slide-up text-sm font-medium ${m[toast.type]||m.info}`}>
      {i[toast.type]||i.info}<span className="flex-1">{toast.msg}</span>
      <button onClick={onClose} className="p-0.5 rounded hover:bg-black/5"><X size={14}/></button>
    </div>
  );
}
