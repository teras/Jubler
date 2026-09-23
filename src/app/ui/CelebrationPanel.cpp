/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/CelebrationPanel.h"

#include <QFontMetrics>
#include <QLinearGradient>
#include <QMouseEvent>
#include <QPainter>
#include <QTimer>
#include <QWindow>
#include <algorithm>
#include <cmath>

namespace {
const char *const MSGS[] = {"Version 11", "21 Years", "All grown up", "21 years of perfect timing", "Still perfectly in sync", "Coming of age, frame by frame",
                            "Always on cue", "Caption this moment", "Roll the credits", "Every second counts", "Reading between the lines",
                            "Fluent in every language", "Down to the millisecond", "Press play on 21"};
const QColor MSG_COLORS[] = {QColor(70, 200, 255), QColor(255, 195, 50), QColor(255, 90, 200), QColor(110, 255, 150), QColor(170, 120, 255), QColor(255, 110, 90), QColor(70, 230, 200),
                             QColor(255, 180, 120), QColor(255, 100, 140), QColor(120, 255, 230), QColor(255, 130, 40), QColor(140, 210, 255), QColor(200, 255, 90), QColor(255, 120, 220)};
const QColor PALETTE[] = {QColor(255, 80, 80), QColor(255, 195, 50), QColor(120, 255, 120), QColor(70, 200, 255), QColor(190, 120, 255), QColor(255, 90, 200), QColor(255, 255, 255)};
constexpr int MSG_COUNT = int(sizeof(MSGS) / sizeof(MSGS[0]));
constexpr int PALETTE_COUNT = int(sizeof(PALETTE) / sizeof(PALETTE[0]));
}  // namespace

CelebrationPanel::CelebrationPanel(QWidget *parent) : QWidget(parent), random_(QRandomGenerator::securelySeeded()) {
    setAutoFillBackground(true);
    QPalette pal = palette();
    pal.setColor(QPalette::Window, Qt::black);
    setPalette(pal);
    timer_ = new QTimer(this);
    timer_->setInterval(16);
    connect(timer_, &QTimer::timeout, this, &CelebrationPanel::tick);
}

void CelebrationPanel::start() { timer_->start(); }
void CelebrationPanel::stop() { timer_->stop(); }

double CelebrationPanel::s() const {
    const int m = std::min(width(), height());
    return m <= 0 ? 1.0 : m / BASE;
}

void CelebrationPanel::mousePressEvent(QMouseEvent *e) {
    launchRocket(e->position().x() / s(), std::max(60.0, e->position().y() / s()), -1);
}

void CelebrationPanel::tick() {
    if (width() <= 0 || height() <= 0 || !isVisible()) return;
    if (window() && window()->isMinimized()) return;
    if (!seeded_) {
        for (int i = 0; i < 140; ++i) confetti_.append(newConfetti(true));
        seeded_ = true;
    }
    int steps;
    if (isActiveWindow()) {
        timer_->setInterval(16);
        stepAcc_ = 0;
        steps = 1;
    } else {
        timer_->setInterval(25);
        stepAcc_ += 1.5;
        steps = int(stepAcc_);
        stepAcc_ -= steps;
    }
    for (int i = 0; i < steps; ++i) step();
    stepsSincePaint_ += steps;
    update();
}

void CelebrationPanel::step() {
    ++tick_;
    if (tick_ % FORM_INTERVAL == 1) launchRocket(wLog() / 2, hLog() * 0.45, nextMsg());
    if (rnd() < 0.03) launchRocket(rnd() * wLog(), hLog() * (0.2 + rnd() * 0.4), -2);
    updateRockets();
    updateParticles();
    updateConfetti();
}

int CelebrationPanel::nextMsg() {
    if (formStep_ < 2) return formStep_++;
    if (bag_.isEmpty()) {
        bag_ << 0 << 0 << 1 << 1;
        for (int i = 2; i < MSG_COUNT; ++i) bag_.append(i);
        std::shuffle(bag_.begin(), bag_.end(), random_);
    }
    return bag_.takeLast();
}

void CelebrationPanel::launchRocket(double x, double targetY, int textMsg) {
    Rocket r;
    r.x = x;
    r.y = hLog();
    r.targetY = targetY;
    r.vx = (rnd() - 0.5) * 1.2;
    r.vy = -(15 + rnd() * 4);
    r.color = textMsg >= 0 ? MSG_COLORS[textMsg] : PALETTE[random_.bounded(PALETTE_COUNT)];
    r.textMsg = textMsg;
    rockets_.append(r);
}

void CelebrationPanel::updateRockets() {
    for (int i = rockets_.size() - 1; i >= 0; --i) {
        Rocket &r = rockets_[i];
        r.x += r.vx;
        r.y += r.vy;
        r.vy += 0.25;
        if (r.vy >= 0 || r.y <= r.targetY) {
            const Rocket done = r;
            rockets_.removeAt(i);
            if (done.textMsg >= 0) spawnTextBurst(QString::fromLatin1(MSGS[done.textMsg]), done.color);
            else spawnBurst(done.x, done.y, done.color, done.textMsg == -2);
        }
    }
}

void CelebrationPanel::spawnBurst(double x, double y, const QColor &base, bool small) {
    const int n = small ? 45 + random_.bounded(30) : 150 + random_.bounded(120);
    const double power = small ? 4.0 : 6.0 + rnd() * 3.5;
    const bool multi = rnd() < 0.4;
    const bool ring = rnd() < 0.5;
    for (int i = 0; i < n; ++i) {
        const double a = rnd() * M_PI * 2;
        const double sp = ring ? power * (0.85 + rnd() * 0.2) : power * rnd();
        Particle p;
        p.x = x;
        p.y = y;
        p.vx = std::cos(a) * sp;
        p.vy = std::sin(a) * sp;
        p.color = multi ? PALETTE[random_.bounded(PALETTE_COUNT)] : jitter(base);
        p.size = 2.5f + float(rnd()) * 2.5f;
        p.decay = 0.008 + rnd() * 0.01;
        p.glitter = rnd() < 0.5;
        particles_.append(p);
    }
}

void CelebrationPanel::spawnTextBurst(const QString &msg, const QColor &col) {
    const double ox = wLog() / 2.0, oy = hLog() * 0.45;
    for (const QPointF &pt : textPoints(msg)) {
        Particle p;
        p.x = ox + (rnd() - 0.5) * BASE * 0.06;
        p.y = oy + (rnd() - 0.5) * BASE * 0.06;
        p.tx = pt.x();
        p.ty = pt.y();
        p.seeking = true;
        p.holdFrames = 95 + random_.bounded(25);
        p.color = jitter(col);
        p.size = 3;
        p.decay = 0.012;
        p.glitter = rnd() < 0.4;
        particles_.append(p);
    }
}

void CelebrationPanel::updateParticles() {
    for (int i = particles_.size() - 1; i >= 0; --i) {
        Particle &p = particles_[i];
        if (p.seeking) {
            p.x += (p.tx - p.x) * 0.14 + (rnd() - 0.5) * 0.6;
            p.y += (p.ty - p.y) * 0.14 + (rnd() - 0.5) * 0.6;
            if (--p.holdFrames <= 0) {
                p.seeking = false;
                p.vx = (rnd() - 0.5) * 1.5;
                p.vy = rnd() * 0.5;
            }
        } else {
            p.vy += 0.12;
            p.vx *= 0.985;
            p.vy *= 0.985;
            p.x += p.vx;
            p.y += p.vy;
            p.life -= p.decay;
            if (p.life <= 0) particles_.removeAt(i);
        }
    }
}

CelebrationPanel::Confetti CelebrationPanel::newConfetti(bool anywhere) {
    Confetti c;
    c.x = rnd() * wLog();
    c.y = anywhere ? rnd() * hLog() : -10;
    c.vx = (rnd() - 0.5) * 0.8;
    c.vy = 2.5 + rnd() * 3.5;
    c.angle = rnd() * M_PI;
    c.spin = (rnd() - 0.5) * 0.3;
    c.w = 8 + rnd() * 8;
    c.h = 5 + rnd() * 5;
    c.phase = rnd() * M_PI * 2;
    c.color = PALETTE[random_.bounded(PALETTE_COUNT)];
    return c;
}

void CelebrationPanel::updateConfetti() {
    const double hl = hLog(), wl = wLog();
    for (int i = 0; i < confetti_.size(); ++i) {
        Confetti &c = confetti_[i];
        c.y += c.vy;
        c.x += c.vx + std::sin(tick_ * 0.05 + c.phase) * 0.7;
        c.angle += c.spin;
        if (c.y > hl + 14) {
            Confetti n = newConfetti(false);
            n.x = rnd() * wl;
            confetti_[i] = n;
        }
    }
}

QList<QPointF> CelebrationPanel::textPoints(const QString &msg) {
    int fontSize = int(BASE * 0.22);
    QFont font(QStringLiteral("Sans Serif"));
    font.setBold(true);
    font.setPixelSize(fontSize);
    QFontMetrics fm(font);
    int tw = fm.horizontalAdvance(msg);
    const double maxW = wLog() * 0.82;
    if (tw > maxW && tw > 0) {
        fontSize = int(fontSize * maxW / tw);
        font.setPixelSize(std::max(1, fontSize));
        fm = QFontMetrics(font);
        tw = fm.horizontalAdvance(msg);
    }
    const int th = fm.height();
    QImage img(std::max(1, tw + 4), std::max(1, th + 4), QImage::Format_ARGB32);
    img.fill(Qt::transparent);
    {
        QPainter g(&img);
        g.setRenderHint(QPainter::Antialiasing);
        g.setRenderHint(QPainter::TextAntialiasing);
        g.setFont(font);
        g.setPen(Qt::white);
        g.drawText(2, 2 + fm.ascent(), msg);
    }
    const int step = std::max(6, fontSize / 13);
    QList<QPointF> pts;
    const double offx = (wLog() - img.width()) / 2.0;
    const double offy = hLog() * 0.45 - img.height() / 2.0;
    for (int y = 0; y < img.height(); y += step)
        for (int x = 0; x < img.width(); x += step)
            if (qAlpha(img.pixel(x, y)) > 128) pts.append(QPointF(x + offx, y + offy));
    std::shuffle(pts.begin(), pts.end(), random_);
    return pts;
}

QColor CelebrationPanel::jitter(const QColor &c) {
    auto j = [this](int v) { return std::clamp(v + int(random_.bounded(61)) - 30, 0, 255); };
    return QColor(j(c.red()), j(c.green()), j(c.blue()));
}

const QImage &CelebrationPanel::glowSprite(const QColor &c, float size) {
    const int rq = c.red() >> 5, gq = c.green() >> 5, bq = c.blue() >> 5;
    const int sq = std::max(1, int(std::lround(size * 2)));
    const int key = ((sq * 8 + rq) * 8 + gq) * 8 + bq;
    auto it = glowCache_.find(key);
    if (it != glowCache_.end()) return it.value();
    const int r = std::min(255, rq * 32 + 16), g = std::min(255, gq * 32 + 16), b = std::min(255, bq * 32 + 16);
    const float sz = (sq / 2.0f) * float(SCENE_SCALE);
    const float halo = sz * 3;
    const int dim = int(std::ceil(halo * 2)) + 2;
    QImage img(dim, dim, QImage::Format_ARGB32_Premultiplied);
    img.fill(Qt::transparent);
    {
        QPainter p(&img);
        p.setRenderHint(QPainter::Antialiasing);
        p.setPen(Qt::NoPen);
        const double cx = dim / 2.0, cy = dim / 2.0;
        p.setBrush(QColor(r, g, b, 255 / 6));
        p.drawEllipse(QPointF(cx, cy), halo, halo);
        p.setBrush(QColor(std::min(255, r + 70), std::min(255, g + 70), std::min(255, b + 70)));
        p.drawEllipse(QPointF(cx, cy), sz, sz);
    }
    return *glowCache_.insert(key, img);
}

void CelebrationPanel::drawGlow(QPainter &g, double x, double y, float size, const QColor &c, int alpha) {
    alpha = std::clamp(alpha, 0, 255);
    if (alpha == 0) return;
    const QImage &sprite = glowSprite(c, size);
    const double half = sprite.width() / 2.0;
    g.setOpacity(std::min(255, (alpha >> 3) * 8 + 4) / 255.0);
    g.drawImage(QPointF(std::lround(x * SCENE_SCALE - half), std::lround(y * SCENE_SCALE - half)), sprite);
    g.setOpacity(1.0);
}

void CelebrationPanel::paintEvent(QPaintEvent *) {
    const int w = width(), h = height();
    if (w <= 0 || h <= 0) return;
    const int sw = int(std::lround(wLog() * SCENE_SCALE)), sh = int(std::lround(hLog() * SCENE_SCALE));
    if (sw < 1 || sh < 1) return;
    if (scene_.isNull() || scene_.width() != sw || scene_.height() != sh) {
        bg_ = QImage(sw, sh, QImage::Format_RGB32);
        {
            QPainter p(&bg_);
            QLinearGradient grad(0, 0, 0, sh);
            grad.setColorAt(0, QColor(22, 10, 42));
            grad.setColorAt(1, QColor(6, 3, 14));
            p.fillRect(bg_.rect(), grad);
        }
        scene_ = bg_.copy();
        stepsSincePaint_ = 0;
    }
    const int n = stepsSincePaint_;
    stepsSincePaint_ = 0;
    if (n > 0) {
        QPainter sg(&scene_);
        const double fade = n <= 1 ? 0.14 : (n == 2 ? 0.2604 : 1 - std::pow(0.86, n));
        sg.setOpacity(fade);
        sg.drawImage(0, 0, bg_);
        sg.setOpacity(1.0);
        for (const Rocket &r : rockets_) drawGlow(sg, r.x, r.y, 5, r.color, 255);
        for (const Particle &p : particles_) {
            int a = int(std::clamp(p.life, 0.0, 1.0) * 255);
            if (p.glitter && rnd() < 0.25) a = int(a * 0.4);
            drawGlow(sg, p.x, p.y, p.size, p.color, a);
        }
    }
    QPainter screen(this);
    screen.setRenderHint(QPainter::SmoothPixmapTransform);
    screen.drawImage(rect(), scene_);
    screen.setRenderHint(QPainter::Antialiasing);
    const double sc = s();
    for (const Confetti &c : confetti_) {
        screen.save();
        screen.translate(c.x * sc, c.y * sc);
        screen.rotate(c.angle * 180 / M_PI);
        screen.fillRect(QRectF(-c.w / 2 * sc, -c.h / 2 * sc, c.w * sc, c.h * sc), c.color);
        screen.restore();
    }
}
