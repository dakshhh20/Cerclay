import React from "react";
import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <section className="not-found">
      <p className="eyebrow">404</p>
      <h1>That page isn't here.</h1>
      <Link className="primary-button" to="/">
        Back home
      </Link>
    </section>
  );
}