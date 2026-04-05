/* ═══════════════════════════════════════════════════════════
   QShield ONE Platform — Main JavaScript
   Animations, interactions, threat map, particle background
   ═══════════════════════════════════════════════════════════ */

(function () {
  'use strict';

  // ─── Particle Background ───
  const canvas = document.getElementById('particle-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    let particles = [];
    const PARTICLE_COUNT = 60;

    function resizeCanvas() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }

    class Particle {
      constructor() {
        this.reset();
      }
      reset() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.vx = (Math.random() - 0.5) * 0.4;
        this.vy = (Math.random() - 0.5) * 0.4;
        this.radius = Math.random() * 1.5 + 0.5;
        this.opacity = Math.random() * 0.4 + 0.1;
        this.color = ['#00d4ff', '#7c3aed', '#06ffa5', '#3b82f6'][Math.floor(Math.random() * 4)];
      }
      update() {
        this.x += this.vx;
        this.y += this.vy;
        if (this.x < 0 || this.x > canvas.width) this.vx *= -1;
        if (this.y < 0 || this.y > canvas.height) this.vy *= -1;
      }
      draw() {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fillStyle = this.color;
        ctx.globalAlpha = this.opacity;
        ctx.fill();
        ctx.globalAlpha = 1;
      }
    }

    function initParticles() {
      particles = [];
      for (let i = 0; i < PARTICLE_COUNT; i++) {
        particles.push(new Particle());
      }
    }

    function drawConnections() {
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 150) {
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(0, 212, 255, ${0.06 * (1 - dist / 150)})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      }
    }

    function animateParticles() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles.forEach(p => { p.update(); p.draw(); });
      drawConnections();
      requestAnimationFrame(animateParticles);
    }

    resizeCanvas();
    initParticles();
    animateParticles();
    window.addEventListener('resize', () => { resizeCanvas(); initParticles(); });
  }

  // ─── Threat Map Canvas ───
  const tmCanvas = document.getElementById('threat-map-canvas');
  if (tmCanvas) {
    const tmCtx = tmCanvas.getContext('2d');
    let threats = [];

    function resizeTm() {
      tmCanvas.width = tmCanvas.parentElement.clientWidth;
      tmCanvas.height = 400;
    }

    function drawWorldOutline() {
      const w = tmCanvas.width, h = tmCanvas.height;
      // Simplified world grid
      tmCtx.strokeStyle = 'rgba(0, 212, 255, 0.06)';
      tmCtx.lineWidth = 0.5;
      for (let x = 0; x < w; x += 40) {
        tmCtx.beginPath(); tmCtx.moveTo(x, 0); tmCtx.lineTo(x, h); tmCtx.stroke();
      }
      for (let y = 0; y < h; y += 40) {
        tmCtx.beginPath(); tmCtx.moveTo(0, y); tmCtx.lineTo(w, y); tmCtx.stroke();
      }
      // Continent blobs (simplified)
      const continents = [
        { x: 0.18, y: 0.32, rx: 0.08, ry: 0.12 },  // North America
        { x: 0.22, y: 0.62, rx: 0.04, ry: 0.1 },   // South America
        { x: 0.47, y: 0.28, rx: 0.06, ry: 0.12 },   // Europe
        { x: 0.52, y: 0.52, rx: 0.08, ry: 0.15 },   // Africa
        { x: 0.68, y: 0.35, rx: 0.1, ry: 0.12 },    // Asia
        { x: 0.82, y: 0.65, rx: 0.04, ry: 0.05 },   // Australia
        { x: 0.62, y: 0.55, rx: 0.04, ry: 0.06 },   // India
      ];
      continents.forEach(c => {
        tmCtx.beginPath();
        tmCtx.ellipse(c.x * w, c.y * h, c.rx * w, c.ry * h, 0, 0, Math.PI * 2);
        tmCtx.fillStyle = 'rgba(0, 212, 255, 0.03)';
        tmCtx.fill();
        tmCtx.strokeStyle = 'rgba(0, 212, 255, 0.08)';
        tmCtx.lineWidth = 1;
        tmCtx.stroke();
      });
    }

    function spawnThreat() {
      const colors = ['#ef4444', '#fb923c', '#8b5cf6', '#00f0ff', '#10b981'];
      const w = tmCanvas.width, h = tmCanvas.height;
      threats.push({
        x: Math.random() * w * 0.7 + w * 0.1,
        y: Math.random() * h * 0.6 + h * 0.15,
        radius: 2,
        maxRadius: Math.random() * 16 + 8,
        color: colors[Math.floor(Math.random() * colors.length)],
        opacity: 0.8,
        speed: Math.random() * 0.3 + 0.15
      });
    }

    function animateThreatMap() {
      tmCtx.clearRect(0, 0, tmCanvas.width, tmCanvas.height);
      drawWorldOutline();
      
      if (Math.random() < 0.08) spawnThreat();
      
      threats = threats.filter(t => t.opacity > 0.01);
      threats.forEach(t => {
        t.radius += t.speed;
        t.opacity = Math.max(0, 0.8 * (1 - t.radius / t.maxRadius));

        tmCtx.beginPath();
        tmCtx.arc(t.x, t.y, t.radius, 0, Math.PI * 2);
        tmCtx.fillStyle = t.color;
        tmCtx.globalAlpha = t.opacity * 0.3;
        tmCtx.fill();

        tmCtx.beginPath();
        tmCtx.arc(t.x, t.y, 2, 0, Math.PI * 2);
        tmCtx.fillStyle = t.color;
        tmCtx.globalAlpha = t.opacity;
        tmCtx.fill();

        tmCtx.globalAlpha = 1;
      });

      requestAnimationFrame(animateThreatMap);
    }

    resizeTm();
    animateThreatMap();
    window.addEventListener('resize', resizeTm);
  }

  // ─── Navigation Scroll Effect ───
  const nav = document.querySelector('.nav');
  if (nav) {
    let lastScroll = 0;
    window.addEventListener('scroll', () => {
      const currentScroll = window.scrollY;
      if (currentScroll > 60) {
        nav.classList.add('scrolled');
      } else {
        nav.classList.remove('scrolled');
      }
      lastScroll = currentScroll;
    }, { passive: true });
  }

  // ─── Mobile Nav Toggle ───
  const hamburger = document.querySelector('.nav-hamburger');
  const navLinks = document.querySelector('.nav-links');
  if (hamburger && navLinks) {
    hamburger.addEventListener('click', () => {
      navLinks.classList.toggle('open');
      hamburger.classList.toggle('active');
    });
    navLinks.addEventListener('click', (e) => {
      if (e.target.tagName === 'A') {
        navLinks.classList.remove('open');
        hamburger.classList.remove('active');
      }
    });
  }

  // ─── Scroll Reveal ───
  const revealElements = document.querySelectorAll('.reveal');
  if (revealElements.length > 0) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

    revealElements.forEach(el => observer.observe(el));
  }

  // ─── Animated Counters ───
  function animateCounter(el, target, suffix = '') {
    const duration = 2000;
    const start = 0;
    const startTime = performance.now();

    function update(currentTime) {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
      const current = Math.floor(start + (target - start) * eased);

      if (target >= 1000000) {
        el.textContent = (current / 1000000).toFixed(1) + 'M' + suffix;
      } else if (target >= 1000) {
        el.textContent = (current / 1000).toFixed(target >= 10000 ? 0 : 1) + 'K' + suffix;
      } else {
        el.textContent = current.toLocaleString() + suffix;
      }

      if (progress < 1) requestAnimationFrame(update);
    }

    requestAnimationFrame(update);
  }

  // Observe stat elements
  const statElements = document.querySelectorAll('[data-count]');
  if (statElements.length > 0) {
    const statObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const target = parseInt(entry.target.dataset.count);
          const suffix = entry.target.dataset.suffix || '';
          animateCounter(entry.target, target, suffix);
          statObserver.unobserve(entry.target);
        }
      });
    }, { threshold: 0.5 });

    statElements.forEach(el => statObserver.observe(el));
  }

  // ─── Pricing Toggle ───
  const pricingToggle = document.querySelector('.pricing-toggle .toggle');
  if (pricingToggle) {
    const monthlyLabel = document.querySelector('.pricing-toggle .monthly');
    const annualLabel = document.querySelector('.pricing-toggle .annual-label');
    const monthlyPrices = document.querySelectorAll('.price-monthly');
    const annualPrices = document.querySelectorAll('.price-annual');

    pricingToggle.addEventListener('click', () => {
      pricingToggle.classList.toggle('annual');
      const isAnnual = pricingToggle.classList.contains('annual');

      if (monthlyLabel) monthlyLabel.classList.toggle('active', !isAnnual);
      if (annualLabel) annualLabel.classList.toggle('active', isAnnual);

      monthlyPrices.forEach(el => el.style.display = isAnnual ? 'none' : '');
      annualPrices.forEach(el => el.style.display = isAnnual ? '' : 'none');
    });
  }

  // ─── Tab System ───
  document.querySelectorAll('.tab-bar').forEach(bar => {
    const buttons = bar.querySelectorAll('.tab-btn');
    const panels = bar.parentElement.querySelectorAll('.tab-panel');

    buttons.forEach(btn => {
      btn.addEventListener('click', () => {
        const target = btn.dataset.tab;
        
        buttons.forEach(b => b.classList.remove('active'));
        panels.forEach(p => p.classList.remove('active'));
        
        btn.classList.add('active');
        const panel = document.getElementById(target);
        if (panel) panel.classList.add('active');
      });
    });
  });

  // ─── Segment Tabs ───
  document.querySelectorAll('.segment-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const group = tab.closest('.segment-tabs');
      if (!group) return;
      group.querySelectorAll('.segment-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const target = tab.dataset.segment;
      const container = tab.closest('.section') || document.body;
      container.querySelectorAll('.segment-content').forEach(c => {
        c.style.display = c.dataset.segment === target ? '' : 'none';
      });
    });
  });

  // ─── Smooth Dropdown for Products Nav ───
  document.querySelectorAll('.dropdown').forEach(dd => {
    let timeout;
    dd.addEventListener('mouseenter', () => {
      clearTimeout(timeout);
      dd.querySelector('.dropdown-menu').style.opacity = '1';
      dd.querySelector('.dropdown-menu').style.visibility = 'visible';
      dd.querySelector('.dropdown-menu').style.transform = 'translateX(-50%) translateY(0)';
    });
    dd.addEventListener('mouseleave', () => {
      timeout = setTimeout(() => {
        const menu = dd.querySelector('.dropdown-menu');
        if (menu) {
          menu.style.opacity = '0';
          menu.style.visibility = 'hidden';
          menu.style.transform = 'translateX(-50%) translateY(10px)';
        }
      }, 150);
    });
  });

  // ─── Current Year ───
  document.querySelectorAll('.current-year').forEach(el => {
    el.textContent = new Date().getFullYear();
  });

  // ─── Active Nav Link ───
  const currentPage = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-links a').forEach(link => {
    const href = link.getAttribute('href');
    if (href && (href === currentPage || href === './' + currentPage)) {
      link.classList.add('active');
    }
  });

})();
