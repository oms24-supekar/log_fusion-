import {statusClass,formatDate} from '../lib/utils';
export const Badge=({value})=><span className={`badge badge-${statusClass(value)}`}>{value||'UNKNOWN'}</span>;
export function PageHeader({eyebrow,title,subtitle,actions}){return <header className="page-header"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{subtitle}</p></div>{actions&&<div>{actions}</div>}</header>}
export function Panel({title,subtitle,actions,children}){return <section className="panel"><div className="panel-head"><div><h2>{title}</h2>{subtitle&&<p>{subtitle}</p>}</div>{actions}</div>{children}</section>}
export const Stat=({label,value,meta,tone=''})=><div className={`stat ${tone}`}><span>{label}</span><strong>{value}</strong><small>{meta}</small></div>;
export const Loading=()=> <div className="loading"><span className="spinner"/>Loading telemetry…</div>;
export const ErrorBox=({message})=>message?<div className="error-box">{message}</div>:null;
export const InfoRow=({label,value})=><div className="info-row"><span>{label}</span><div>{value??'—'}</div></div>;
export const Confidence=({value})=>{if(value==null)return <span>—</span>;const p=Math.round(Number(value)*100);return <span className={`confidence ${p>=85?'good':p>=60?'warn':'bad'}`}>{p}%</span>};
export function ReviewField({label,value,review}){return <div className={`review-field ${review?.suspicious?'suspicious':''}`} title={review?`${Math.round(review.confidence*100)}% — ${review.reason}`:''}><span>{label}</span><strong>{value??'—'}</strong>{review&&<Confidence value={review.confidence}/>}</div>}
export const Time=({value})=><>{formatDate(value)}</>;
