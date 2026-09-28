export const formatDate=v=>{if(!v)return '—';const d=new Date(v);return Number.isNaN(d.getTime())?String(v):d.toLocaleString()};
export const statusClass=v=>String(v||'unknown').toLowerCase().replaceAll('_','-').replaceAll(' ','-');
export function unwrapNormalized(n){const d=n?.normalizedData??n?.normalized??n;if(!d||typeof d!=='object')return{event:null,aiReview:null,rawStored:d};return d.normalized?{event:d.normalized,aiReview:d.aiReview||null,rawStored:d}:{event:d,aiReview:d.aiReview||null,rawStored:d};}
export function reviewMap(r){const m=new Map();for(const x of r?.fields||[])if(x?.field)m.set(x.field,x);return m;}
export function countBy(items,fn){const o={};for(const x of items||[]){const k=fn(x)||'UNKNOWN';o[k]=(o[k]||0)+1}return o;}
