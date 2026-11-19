import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Ellipse, Rectangle

fig, ax = plt.subplots(figsize=(15.5, 9), dpi=160)
ax.set_xlim(0, 15.6); ax.set_ylim(0, 9); ax.axis("off")

def box(x, y, w, h, color, title, sub=None, port=None, fs=13):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.12",
                                fc=color, ec="none"))
    ty = y + h/2 + (0.2 if sub else 0)
    ax.text(x + w/2, ty, title, ha="center", va="center", color="white", fontsize=fs, weight="bold")
    if sub:
        ax.text(x + w/2, y + h/2 - 0.17, sub, ha="center", va="center", color="white", fontsize=9.5)
    if port:
        ax.text(x + w - 0.08, y + 0.1, port, ha="right", va="bottom", color="white",
                fontsize=10.5, family="monospace", weight="bold")

def cyl(cx, cy, label):
    w, h = 0.75, 0.85
    ax.add_patch(Rectangle((cx - w/2, cy - h/2), w, h, fc="#0D2A8A", ec="none"))
    ax.add_patch(Ellipse((cx, cy - h/2), w, 0.28, fc="#0D2A8A", ec="none"))
    ax.add_patch(Ellipse((cx, cy + h/2), w, 0.28, fc="#2B4DB8", ec="#0D2A8A", lw=1.5))
    ax.text(cx, cy - h/2 - 0.35, label, ha="center", va="top", fontsize=9.5, weight="bold", color="#0D2A8A")

def arrow(x1, y1, x2, y2, color, ls="-", lw=1.8, both=False):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="<|-|>" if both else "-|>", color=color, lw=lw, ls=ls,
                                shrinkA=0, shrinkB=0, mutation_scale=14))

# Title
ax.text(7.75, 8.75, "Credi-Go — Architecture micro-services", ha="center", fontsize=16, weight="bold")

# System container
ax.add_patch(FancyBboxPatch((3.0, 0.3), 12.45, 8.0, boxstyle="round,pad=0.02,rounding_size=0.2",
                            fc="#EAF4FC", ec="#B8D8F0", lw=1.5))

# Client
box(0.25, 3.05, 2.2, 1.1, "#E8700F", "Client HTTP", "Postman / Frontend")
arrow(2.45, 3.6, 3.55, 3.6, "#E8700F", lw=2.2, both=True)

# Gateway
box(3.55, 2.95, 2.3, 1.3, "#5E9C3A", "API Gateway", "gateway-service", ":8080")

# Eureka
box(7.9, 6.75, 2.9, 1.15, "#4CC24F", "Service Registry", "Eureka — service-registry", ":8761")

# Business services
svc = [
    (4.9, "service-clients", "Client, Collateral", ":8081", "#0B6FD0", "clientsdb"),
    (3.0, "service-credit ★", "LoanApplication, RepaymentSchedule", ":8082", "#0A58AE", "creditdb"),
    (1.1, "service-decaissements", "Disbursement, TreasuryAccount", ":8083", "#0B6FD0", "decaissementsdb"),
]
sx, sw, sh = 7.9, 4.0, 1.2
for y, name, sub, port, col, db in svc:
    box(sx, y, sw, sh, col, name, sub, port, fs=12.5)
    cy = y + sh/2
    # gateway routing (blue)
    ax.plot([5.85, 6.2, 6.2], [3.6, 3.6, cy], color="#2E6FB7", lw=1.8)
    arrow(6.2, cy, sx, cy, "#2E6FB7")
    # database
    cyl(14.3, cy + 0.05, f"H2 · {db}")
    ax.plot([sx + sw, 13.92], [cy, cy], color="#0D2A8A", lw=1.8)
    ax.plot(13.92, cy, "o", color="#0D2A8A", ms=5)

# Route labels
for (y, *_), route in zip(svc, ["/clients/**", "/credits/**", "/decaissements/**"]):
    ax.text(6.3, y + sh/2 + 0.08, route, fontsize=8.5, color="#2E6FB7", family="monospace", va="bottom")

# Eureka registration bus (dashed green)
bx = 12.45
ax.plot([bx, bx], [svc[2][0] + sh/2 + 0.35, 7.3], color="#4C9A2A", lw=1.6, ls="--")
for y, *_ in svc:
    ax.plot([sx + sw, bx], [y + sh/2 + 0.35, y + sh/2 + 0.35], color="#4C9A2A", lw=1.4, ls="--")
arrow(bx, 7.3, 10.8, 7.3, "#4C9A2A", ls="--", lw=1.6)
ax.text(10.95, 7.15, "enregistrement\n(Eureka Client)", fontsize=9, color="#4C9A2A", va="top")


# Config Server (V3)
box(13.05, 6.75, 2.05, 1.15, "#7E5FA6", "Config Server", "config-repo/", ":8888", fs=12)
ax.annotate("", xy=(13.05, 6.95), xytext=(12.45, 6.95),
            arrowprops=dict(arrowstyle="<|-", color="#7E5FA6", lw=1.4, ls=":", mutation_scale=12))
ax.text(14.07, 6.55, "configuration chargée\nau démarrage", ha="center", va="top", fontsize=8.5, color="#7E5FA6")

# Gateway discovery
ax.plot([4.7, 4.7], [4.25, 7.3], color="#4C9A2A", lw=1.6, ls="--")
arrow(4.7, 7.3, 7.9, 7.3, "#4C9A2A", ls="--", lw=1.6)
ax.text(4.85, 7.4, "découverte (lb://)", fontsize=9, color="#4C9A2A")

# Legend
ax.plot([3.3, 3.8], [0.6, 0.6], color="#2E6FB7", lw=1.8); ax.text(3.9, 0.6, "routage HTTP via la Gateway", va="center", fontsize=9)
ax.plot([6.8, 7.3], [0.6, 0.6], color="#4C9A2A", lw=1.6, ls="--"); ax.text(7.4, 0.6, "service discovery (Eureka)", va="center", fontsize=9)
ax.plot([10.1, 10.6], [0.6, 0.6], color="#0D2A8A", lw=1.8); ax.text(10.7, 0.6, "base propre au service", va="center", fontsize=9)
ax.plot([13.0, 13.5], [0.6, 0.6], color="#7E5FA6", lw=1.4, ls=":"); ax.text(13.6, 0.6, "configuration", va="center", fontsize=9)

plt.savefig("architecture.png", bbox_inches="tight", facecolor="white")
