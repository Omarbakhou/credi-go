import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Toutes les requêtes passent par l'API Gateway (routage dynamique via Eureka). */
const GATEWAY = 'http://localhost:8080';

export interface Client { id: number; nom: string; email: string; revenusMensuels: number; scoreInterne: number; statut: string; }
export interface Dossier {
  id: number; clientId: number; montantDemande: number; montantAccorde: number | null; scoreEtude: number | null;
  dureeMois: number; tauxPropose: number | null; motif: string; commentaireDecision: string | null; statut: string;
}
export interface Echeance { numeroEcheance: number; dateEcheance: string; montantCapital: number; montantInterets: number; statut: string; }
export interface LigneAmortissement extends Echeance { mensualite: number; capitalRestant: number; }
export interface Tresorerie { id: number; libelle: string; soldeDisponible: number; }
export interface Notification { date: string; evenement: string; dossierId: number; destinataire: string; objet: string; statut: string; }

type Onglet = 'dossiers' | 'clients' | 'tresorerie' | 'notifications';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly http = inject(HttpClient);

  readonly onglet = signal<Onglet>('dossiers');
  readonly clients = signal<Client[]>([]);
  readonly dossiers = signal<Dossier[]>([]);
  readonly tresoreries = signal<Tresorerie[]>([]);
  readonly notifications = signal<Notification[]>([]);
  readonly amortissement = signal<LigneAmortissement[]>([]);
  readonly dossierAmortissement = signal<Dossier | null>(null);
  readonly message = signal<{ type: 'ok' | 'err'; texte: string } | null>(null);
  readonly enCours = signal(false);

  nouvelle = { clientId: 2, montantDemande: 20000, dureeMois: 24, motif: 'Achat électroménager' };

  constructor() {
    this.rafraichir();
  }

  // ---------- Lecture ----------
  rafraichir(): void {
    this.http.get<Client[]>(`${GATEWAY}/service-clients/clients`).subscribe({ next: c => this.clients.set(c), error: e => this.erreur(e) });
    this.http.get<Dossier[]>(`${GATEWAY}/service-credit/credits`).subscribe({ next: d => this.dossiers.set([...d].sort((a, b) => b.id - a.id)), error: e => this.erreur(e) });
    this.http.get<Tresorerie[]>(`${GATEWAY}/service-decaissements/tresoreries`).subscribe({ next: t => this.tresoreries.set(t), error: e => this.erreur(e) });
    this.http.get<Notification[]>(`${GATEWAY}/notification-service/notifications`).subscribe({ next: n => this.notifications.set(n), error: () => this.notifications.set([]) });
  }

  nomClient(id: number): string {
    return this.clients().find(c => c.id === id)?.nom ?? `Client ${id}`;
  }

  // ---------- Cycle de vie du dossier : DEPOSE -> EN_ETUDE -> ACCORDE -> DEBLOQUE ----------
  deposer(): void {
    const headers = new HttpHeaders({ 'Idempotency-Key': crypto.randomUUID() });
    this.action(this.http.post<Dossier>(`${GATEWAY}/service-credit/credits`, this.nouvelle, { headers }),
      d => `Demande n° ${d.id} déposée (statut ${d.statut}).`);
  }

  etudier(d: Dossier): void {
    this.action(this.http.post<Dossier>(`${GATEWAY}/service-credit/credits/${d.id}/etude`, {}),
      r => `Dossier n° ${r.id} à l'étude, score relevé via OpenFeign : ${r.scoreEtude}.`);
  }

  decider(d: Dossier): void {
    this.action(this.http.post<Dossier>(`${GATEWAY}/service-credit/credits/${d.id}/decision`, {}),
      r => r.statut === 'ACCORDE'
        ? `Dossier n° ${r.id} ACCORDÉ : ${r.montantAccorde} MAD au taux de ${r.tauxPropose} %. Notification envoyée au client.`
        : `Dossier n° ${r.id} REFUSÉ : ${r.commentaireDecision}`);
  }

  verser(d: Dossier): void {
    const headers = new HttpHeaders({ 'Idempotency-Key': crypto.randomUUID() });
    this.action(this.http.post<{ id: number; montant: number; statut: string }>(`${GATEWAY}/service-decaissements/decaissements`,
      { loanApplicationId: d.id, treasuryAccountId: 1 }, { headers }),
      v => `Versement n° ${v.id} : ${v.montant} MAD ${v.statut}. Dossier débloqué, échéancier activé, email envoyé.`);
  }

  // ---------- Tableau d'amortissement ----------
  voirAmortissement(d: Dossier): void {
    this.http.get<Echeance[]>(`${GATEWAY}/service-credit/credits/${d.id}/echeancier`).subscribe({
      next: lignes => {
        let restant = lignes.reduce((s, l) => s + l.montantCapital, 0);
        this.amortissement.set(lignes.map(l => {
          restant = Math.round((restant - l.montantCapital) * 100) / 100;
          return { ...l, mensualite: Math.round((l.montantCapital + l.montantInterets) * 100) / 100, capitalRestant: Math.max(restant, 0) };
        }));
        this.dossierAmortissement.set(d);
      },
      error: e => this.erreur(e),
    });
  }

  fermerAmortissement(): void {
    this.dossierAmortissement.set(null);
    this.amortissement.set([]);
  }

  totalInterets(): number {
    return Math.round(this.amortissement().reduce((s, l) => s + l.montantInterets, 0) * 100) / 100;
  }

  totalMensualites(): number {
    return Math.round(this.amortissement().reduce((s, l) => s + l.mensualite, 0) * 100) / 100;
  }

  // ---------- Utilitaires ----------
  private action<T>(appel: Observable<T>, texte: (r: T) => string): void {
    this.enCours.set(true);
    appel.subscribe({
      next: r => { this.message.set({ type: 'ok', texte: texte(r) }); this.enCours.set(false); setTimeout(() => this.rafraichir(), 400); },
      error: e => { this.erreur(e); this.enCours.set(false); },
    });
  }

  private erreur(e: any): void {
    const corps = e?.error;
    const texte = e?.status === 0
      ? 'API Gateway injoignable sur localhost:8080 (services en cours de démarrage ?). Relancer ./start-all.sh puis cliquer sur Rafraîchir.'
      : corps?.status && corps?.message
        ? `${corps.status} ${corps.erreur} : ${corps.message}${corps.details ? ' (' + Object.values(corps.details).join(', ') + ')' : ''}`
        : `Erreur HTTP ${e?.status ?? ''}`;
    this.message.set({ type: 'err', texte });
  }
}
