/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QColor>
#include <QHash>
#include <QImage>
#include <QList>
#include <QPointF>
#include <QRandomGenerator>
#include <QWidget>

class QTimer;

// The animated "Version 11 / 21 years" fireworks-and-confetti panel shown
// in the first window until a document appears. Port of `JCelebrationPanel`.
class CelebrationPanel : public QWidget {
    Q_OBJECT
public:
    explicit CelebrationPanel(QWidget *parent = nullptr);
    void start();
    void stop();

protected:
    void paintEvent(QPaintEvent *) override;
    void mousePressEvent(QMouseEvent *e) override;

private:
    struct Rocket { double x, y, vx, vy, targetY; QColor color; int textMsg; };
    struct Particle { double x, y, vx = 0, vy = 0, tx = 0, ty = 0, life = 1, decay; bool seeking = false; int holdFrames = 0; QColor color; float size; bool glitter; };
    struct Confetti { double x, y, vx, vy, angle, spin, w, h, phase; QColor color; };

    static constexpr double BASE = 1000;
    static constexpr double SCENE_SCALE = 0.6;
    static constexpr int FORM_INTERVAL = 230;

    double s() const;
    double wLog() const { return width() / s(); }
    double hLog() const { return height() / s(); }
    void tick();
    void step();
    int nextMsg();
    void launchRocket(double x, double targetY, int textMsg);
    void updateRockets();
    void spawnBurst(double x, double y, const QColor &base, bool small);
    void spawnTextBurst(const QString &msg, const QColor &col);
    void updateParticles();
    Confetti newConfetti(bool anywhere);
    void updateConfetti();
    QList<QPointF> textPoints(const QString &msg);
    void drawGlow(QPainter &g, double x, double y, float size, const QColor &c, int alpha);
    const QImage &glowSprite(const QColor &c, float size);
    QColor jitter(const QColor &c);
    double rnd() { return random_.generateDouble(); }

    QTimer *timer_;
    QRandomGenerator random_;
    QList<Rocket> rockets_;
    QList<Particle> particles_;
    QList<Confetti> confetti_;
    QHash<int, QImage> glowCache_;
    QImage scene_, bg_;
    long tick_ = 0;
    int formStep_ = 0;
    QList<int> bag_;
    bool seeded_ = false;
    int stepsSincePaint_ = 0;
    double stepAcc_ = 0;
};
