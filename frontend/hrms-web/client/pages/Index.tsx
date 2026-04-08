import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";

import "./index-redesign.css";

export default function Index() {
  return (
    <div className="main-redesign">
      <div className="page-shell">
        <section className="hero">
          <div className="hero-orb orb-left"></div>
          <div className="hero-orb orb-center"></div>
          <div className="hero-orb orb-right"></div>
          <div className="hero-orb orb-small"></div>

          <div className="container">
            <nav className="topbar glass-pill">
              <div className="brand-wrap">
                <span className="brand-dot">HR</span>
                <span className="brand-text">HRMS System</span>
              </div>
              <div className="nav-links">
                <Link to="/signup">Sign up</Link>
                <span className="divider"></span>
                <Link to="/login">Login</Link>
              </div>
            </nav>

            <div className="hero-content">
              <div className="hero-copy">
                <h1>
                  Everything You Need to <br />
                  Manage HR in Kazakhstan
                </h1>
                <p>
                  Automated payroll, attendance tracking, and <br/>employee management
                  built for local tax compliance.
                </p>
                <div className="hero-actions">
                  <Link to="/signup" className="btn btn-primary">
                    Get Started <ArrowRight className="ml-2 h-4 w-4" />
                  </Link>
                  <a href="#learn-more" className="btn btn-light">
                    Learn more
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="section section-soft" id="learn-more">
          <div className="container narrow">
            <div className="section-heading center">
              <h2>Powerful features for your business</h2>
              <p>Built specifically for Kazakhstan SMEs</p>
            </div>

            <div className="feature-grid">
              <article className="feature-card glow-mint">
                <div className="feature-icon">
                  <img src="/feature-icon-6.png" alt="" />
                </div>
                <h3>Automated Payroll</h3>
                <p>
                  Calculate salaries with all deductions handled automatically.
                </p>
              </article>
              <article className="feature-card glow-blue">
                <div className="feature-icon">
                  <img src="/feature-icon-4.png" alt="" />
                </div>
                <h3>Attendance Tracking</h3>
                <p>Real-time attendance system with smart security checks.</p>
              </article>
              <article className="feature-card glow-lilac">
                <div className="feature-icon">
                  <img src="/feature-icon-5.png" alt="" />
                </div>
                <h3>Employee Management</h3>
                <p>Manage employees from hiring to leaving in one system.</p>
              </article>
              <article className="feature-card glow-peach">
                <div className="feature-icon">
                  <img src="/feature-icon-1.png" alt="" />
                </div>
                <h3>Leave Management</h3>
                <p>Track employee leave and balances automatically.</p>
              </article>
              <article className="feature-card glow-pink">
                <div className="feature-icon">
                  <img src="/feature-icon-2.png" alt="" />
                </div>
                <h3>Real-time Reports</h3>
                <p>Generate tax and payroll reports instantly.</p>
              </article>
              <article className="feature-card glow-violet">
                <div className="feature-icon">
                  <img src="/feature-icon-3.png" alt="" />
                </div>
                <h3>Enterprise Security</h3>
                <p>Secure system with protected access and encrypted data.</p>
              </article>
            </div>
          </div>
        </section>

        <section className="section section-white">
          <div className="container narrow">
            <div className="section-heading center">
              <h2>Why Our HRMS Works Better</h2>
              <p>Built for speed, accuracy, and compliance in Kazakhstan</p>
            </div>

            <div className="stats-layout">
              <article className="showcase-card">
                <div className="showcase-media">
                  <img src="/showcase-image.png" alt="" />
                </div>
                <div className="showcase-body">
                  <h3>+ 500 employees in under 10 min</h3>
                  <p>
                    Handle payroll and employee records in minutes, not hours.
                  </p>
                </div>
              </article>

              <div className="stats-grid">
                <article className="stat-card indigo">
                  <strong>99.5%</strong>
                  <span>System uptime and zero mistakes on salary</span>
                </article>
                <article className="stat-card sky">
                  <strong>24/7</strong>
                  <span>Audit & reports available anytime</span>
                </article>
                <article className="stat-card pink">
                  <strong>zero</strong>
                  <span>Critical security incidents guaranteed</span>
                </article>
                <article className="stat-card mint">
                  <strong>10-500</strong>
                  <span>Employees — built for growing teams</span>
                </article>
              </div>
            </div>
          </div>
        </section>

        <section className="cta-band">
          <div className="container cta-inner">
            <h2>Ready to Transform Your HR?</h2>
            <p>
              Join hundreds of Kazakhstan SMEs already using HRMS to streamline
              their HR operations.
            </p>
            <Link to="/signup" className="btn btn-light btn-cta">
              Start Your Free Trial <ArrowRight className="ml-2 h-4 w-4" />
            </Link>
          </div>
        </section>

        <footer className="footer">
          <div className="container footer-grid">
            <div>
              <h4>Product</h4>
              <a href="#">Features</a>
              <a href="#">Pricing</a>
              <a href="#">Security</a>
            </div>
            <div>
              <h4>Company</h4>
              <a href="#">About</a>
              <a href="#">Blog</a>
              <a href="#">Careers</a>
            </div>
            <div>
              <h4>Legal</h4>
              <a href="#">Privacy</a>
              <a href="#">Terms</a>
              <a href="#">Compliance</a>
            </div>
            <div>
              <h4>Contact</h4>
              <a href="mailto:support@hrms.kz">support@hrms.kz</a>
              <a href="tel:+77223456789">+7 (777) 516-9299</a>
            </div>
          </div>
          <div className="container footer-bottom">
            <p>
              © 2026 HRMSystem. All rights reserved. Built with ❤ for Kazakhstan
              SMEs.
            </p>
          </div>
        </footer>
      </div>
    </div>
  );
}

