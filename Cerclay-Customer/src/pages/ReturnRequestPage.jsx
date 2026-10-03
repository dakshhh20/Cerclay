import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createCustomerReturn, getOrder, uploadReturnPhotos } from "../lib/api";
import { useAuth } from "../context/AuthContext";

const reasons = [
  ["DAMAGED", "Item arrived damaged"],
  ["DEFECTIVE", "Item is defective"],
  ["WRONG_ITEM", "Wrong item received"],
  ["MISSING_ITEM", "Item or part is missing"],
];
function money(value){return `₹${Number(value||0).toLocaleString("en-IN",{minimumFractionDigits:2,maximumFractionDigits:2})}`;}
export default function ReturnRequestPage(){
  const {orderId}=useParams(); const navigate=useNavigate(); const {status}=useAuth();
  const [order,setOrder]=useState(null); const [selected,setSelected]=useState({}); const [reason,setReason]=useState("DAMAGED"); const [note,setNote]=useState(""); const [files,setFiles]=useState([]); const [loading,setLoading]=useState(true); const [busy,setBusy]=useState(false); const [error,setError]=useState("");
  useEffect(()=>{ if(status==="loading") return; getOrder(orderId).then(setOrder).catch(e=>setError(e.message||"Unable to load this order.")).finally(()=>setLoading(false)); },[status,orderId]);
  const selectedItems=useMemo(()=>Object.entries(selected).filter(([,q])=>q>0).map(([id,q])=>({orderItemId:Number(id),quantity:q})),[selected]);
  const estimated=useMemo(()=> (order?.items||[]).reduce((sum,item)=>sum+Number(item.unitPrice||0)*Number(selected[item.id]||0),0),[order,selected]);
  function toggle(item){setSelected(s=>{const n={...s}; if(n[item.id]) delete n[item.id]; else n[item.id]=1; return n;});}
  function quantity(item,delta){setSelected(s=>({...s,[item.id]:Math.max(0,Math.min(item.quantity||1,(s[item.id]||0)+delta))}));}
  async function submit(e){e.preventDefault();setError(""); if(!selectedItems.length)return setError("Select at least one item to return."); if(!files.length)return setError("Please upload at least one photo of the item or package."); setBusy(true); try{const rr=await createCustomerReturn(orderId,{reason,customerNote:note.trim(),items:selectedItems}); await uploadReturnPhotos(rr.id,files); navigate(`/account/returns/${rr.id}`,{replace:true});}catch(err){setError(err.message||"Unable to submit the return request.");}finally{setBusy(false);}}
  if(loading)return <section className="page-container state-page"><p>Loading return details…</p></section>;
  if(error&&!order)return <section className="page-container state-page"><div className="state-box error-state"><strong>Unable to start return</strong><span>{error}</span><Link className="secondary-button" to={`/account/orders/${orderId}`}>Back to order</Link></div></section>;
  if(!order)return null;
  const eligible=String(order.orderStatus||"").toUpperCase()==="DELIVERED" && order.deliveredAt && (new Date(order.deliveredAt).getTime()+2*86400000>=Date.now());
  return <section className="return-page page-container">
    <div className="order-detail-heading"><div><p className="eyebrow">RETURN REQUEST</p><h1>Return order {order.orderNumber}</h1><p>Returns are accepted within 2 days of delivery for eligible issues.</p></div><Link className="secondary-button" to={`/account/orders/${orderId}`}>Back to order</Link></div>
    {!eligible?<div className="state-box error-state"><strong>This order is not currently eligible for return.</strong><span>Returns are available for 2 days after delivery and only for damaged, defective, wrong or missing items.</span></div>:
    <form className="return-form" onSubmit={submit}>
      <section className="order-detail-card"><div className="checkout-card-heading"><div><p className="eyebrow">01</p><h2>Select items</h2></div></div><div className="return-items">{(order.items||[]).map(item=><div className="return-item-row" key={item.id}><label className="return-item-select"><input type="checkbox" checked={!!selected[item.id]} onChange={()=>toggle(item)}/><span><strong>{item.productName}</strong><small>SKU: {item.productSku||"—"} · {money(item.unitPrice)} each</small></span></label>{selected[item.id]&&<div className="return-qty"><button type="button" onClick={()=>quantity(item,-1)}>−</button><strong>{selected[item.id]}</strong><button type="button" onClick={()=>quantity(item,1)}>+</button></div>}</div>)}</div></section>
      <section className="order-detail-card"><div className="checkout-card-heading"><div><p className="eyebrow">02</p><h2>Why are you returning it?</h2></div></div><div className="return-reasons">{reasons.map(([value,label])=><label key={value} className={reason===value?"return-reason selected":"return-reason"}><input type="radio" name="reason" value={value} checked={reason===value} onChange={e=>setReason(e.target.value)}/><span>{label}</span></label>)}</div><label className="return-field"><span>Additional details</span><textarea value={note} onChange={e=>setNote(e.target.value)} maxLength={1000} placeholder="Tell us what happened…"/></label></section>
      <section className="order-detail-card"><div className="checkout-card-heading"><div><p className="eyebrow">03</p><h2>Upload photos</h2><p className="return-help">Please upload at least one clear photo. Up to 5 JPG, PNG or WEBP images, 5 MB each.</p></div></div><input className="return-file-input" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={e=>setFiles(Array.from(e.target.files||[]).slice(0,5))}/>{files.length>0&&<div className="return-file-list">{files.map(f=><span key={`${f.name}-${f.size}`}>{f.name}</span>)}</div>}</section>
      <section className="return-submit-card"><div><span>Estimated refund</span><strong>{money(estimated)}</strong><small>Refund will be issued to the original payment method. Return shipping is free for approved returns.</small></div>{error&&<div className="form-error" role="alert">{error}</div>}<button className="primary-button" disabled={busy}>{busy?"Submitting return…":"Submit return request"}</button></section>
    </form>}
  </section>;
}
