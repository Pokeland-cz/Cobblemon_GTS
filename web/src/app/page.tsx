'use client';

import { useState, useEffect } from 'react';
import { ShoppingCart, LogIn, LogOut, CheckCircle2, XCircle, Search, Sparkles, Coins, RefreshCw, SlidersHorizontal, MessageSquare, Store, Trash2, Archive, Package, PackageOpen } from 'lucide-react';

// Helper for splitting known Cobblemon string IDs into nicely formatted words
const knownSuffixes = [
  "eyes", "rod", "bounce", "guard", "power", "swim", "rush", "surfer", "absorb", "fire", "fat", "head", "grace", "link", "fist", "lens", "scale", "heal", "body", "spore", "luck", "skin", "barbs", "smoke", "boost", "heart", "surge", "stream", "warning", "bond", "construct", "tag", "trap", "pull", "cure", "focus", "breaker", "amulet", "spirit", "bubble", "veil", "gift", "change", "eye", "hold", "cups", "dust", "tempo", "bird", "dish", "feet", "filter", "rock", "mode", "star", "aura", "claws", "launcher", "coat", "jaw", "wings", "majesty", "hair", "down", "compaction", "alchemy", "system", "shield", "armor", "sword", "fetch", "tail", "missile", "engine", "spit", "scales", "face", "sipper", "zipper",
  "whip", "attack", "punch", "kick", "slam", "beam", "dance", "song", "wind", "storm", "spin", "fang", "claw", "bite", "strike", "smash", "shot", "wave", "blast", "throw", "toss", "drop", "horn", "spikes", "gem", "seed", "leaf", "blade", "slash", "sting", "shock", "bolt", "thunder", "spark", "volt", "charge", "laser", "pulse", "force", "trick", "room", "zone", "wish", "kiss", "drain", "swallow", "slurp", "lick", "glow", "light", "flash", "flare", "burn", "flame", "blaze", "water", "hydro", "aqua", "rain", "ice", "snow", "frost", "blizzard", "freeze", "chill", "cold", "heat", "sun", "solar", "moon", "meteor", "comet", "earth", "quake", "magnitude", "land", "mud", "dirt", "sand", "boulder", "stealth", "iron", "steel", "metal", "gear", "magnet", "wing", "fly", "sky", "air", "aero", "bird", "feather", "peck", "drill", "pin", "needle", "bone", "club", "wood", "tree", "forest", "jungle", "vine", "root", "thorn", "flower", "petal", "pollen", "powder", "web", "string", "silk", "thread", "bug", "insect", "poison", "venom", "toxic", "acid", "sludge", "smog", "gas", "shadow", "dark", "night", "black", "ghost", "phantom", "spook", "scare", "fear", "terror", "dread", "horror", "panic", "psychic", "mind", "brain", "dream", "sleep", "rest", "nap", "doze", "snore", "wake", "alert", "aware", "sense", "love", "hate", "anger", "rage", "fury", "wrath", "mad", "crazy", "insane", "wild", "fierce", "savage", "brutal", "cruel", "evil", "wicked", "bad", "naughty", "prank", "joke", "laugh", "smile", "grin", "smirk", "sneer", "mock", "taunt", "tease", "bully", "scold", "nag", "whine", "weep", "sob", "tear", "slide", "glide", "skate", "ski", "surf", "dive", "sink", "drown", "float", "soar", "hover", "drift", "blow", "puff", "pant", "gasp", "choke", "cough", "sneeze", "sweat", "bleed", "vomit", "puke", "hurl", "pitch", "catch", "grab", "keep", "save", "protect", "guard", "defend", "block", "parry", "dodge", "evade", "escape", "flee", "run", "dash", "sprint", "rush", "charge", "hit", "beat", "slap", "smack", "bash", "crash", "break", "destroy", "ruin", "wreck", "spoil", "waste", "lose", "fail", "miss", "die", "perish", "fade", "vanish", "leave", "depart", "go", "move", "travel", "journey", "trip", "tour", "visit", "explore", "discover", "find", "seek", "search", "hunt", "chase", "follow", "track", "trail", "lead", "guide", "show", "tell", "say", "speak", "talk", "chat", "gossip", "whisper", "shout", "yell", "scream", "call", "title", "word", "letter", "sign", "mark", "symbol", "note", "music", "sound", "noise", "voice", "tone", "pitch", "rhythm", "pace", "rate", "time", "clock", "watch", "hour", "minute", "second", "day", "morning", "evening", "noon", "midnight", "dawn", "dusk", "sunrise", "sunset", "spring", "summer", "autumn", "winter", "season", "year", "month", "week", "decade", "century", "millennium", "past", "present", "future", "history", "memory"
].sort((a, b) => b.length - a.length);

const formatCobblemonString = (str: string) => {
  if (!str) return '';
  const s = str.replace('cobblemon:', '').toLowerCase();

  for (const suffix of knownSuffixes) {
    if (s.endsWith(suffix) && s !== suffix) {
      const prefix = s.slice(0, -suffix.length);
      return prefix.charAt(0).toUpperCase() + prefix.slice(1) + " " + suffix.charAt(0).toUpperCase() + suffix.slice(1);
    }
  }
  return s.charAt(0).toUpperCase() + s.slice(1);
};

// Internal Cobblemon features that mean nothing to buyers.
const HIDDEN_FEATURES = new Set(['spawn_bucket', 'region_bias', 'blocks_traveled', 'fertility', 'dynamax_level']);

// Regional form flags and the matching sprite suffix.
const REGIONAL_FORMS = [
  { flag: 'alolan', suffix: 'alola' },
  { flag: 'galarian', suffix: 'galar' },
  { flag: 'hisuian', suffix: 'hisui' },
  { flag: 'paldean', suffix: 'paldea' },
];

// Cobblemon saves each feature as { "cobblemon:feature_id": name, [name]: value }.
// Flags are 0/1 (e.g. Pikachu always has "alolan": 0), strings hold a choice
// ("none" when unset), numbers are counters. Show only flags that are on and set choices.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const describePokemon = (pkm: any) => {
  const speciesName = (pkm.Species || '').replace('cobblemon:', '');
  const formId: string = pkm.FormId && pkm.FormId !== 'normal' ? pkm.FormId : '';
  const displayFeatures: string[] = [];
  const activeFlags: string[] = [];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (pkm.Features || []).forEach((f: any) => {
    const fId = (f['cobblemon:feature_id'] || '').replace('cobblemon:', '');
    if (!fId || HIDDEN_FEATURES.has(fId)) return;
    const value = f[fId];
    let label = '';
    if (value === true || value === 1) {
      label = fId;
      activeFlags.push(fId);
    } else if (typeof value === 'string' && value !== '' && value !== 'none') {
      label = value;
    }
    if (label && !displayFeatures.includes(label)) {
      displayFeatures.push(label);
    }
  });

  // Regional forms are saved twice: as the form ("hisui") and as a flag ("hisuian": 1).
  // Show the form only when no flag already says the same.
  const region = REGIONAL_FORMS.find(r => activeFlags.includes(r.flag) || formId.toLowerCase().includes(r.suffix));
  const formIsRegionFlag = !!region && activeFlags.includes(region.flag) && formId.toLowerCase().includes(region.suffix);
  if (formId && !formIsRegionFlag) displayFeatures.unshift(formId);

  // Sprite name, e.g. rattata-alola
  let spriteName = speciesName.toLowerCase();
  if (region) spriteName += `-${region.suffix}`;

  return { speciesName, displayFeatures, spriteName };
};

const spriteUrl = (spriteName: string, shiny: boolean) =>
  `https://play.pokemonshowdown.com/sprites/${shiny ? 'gen5-shiny' : 'gen5'}/${spriteName}.png`;

const FALLBACK_SPRITE = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png';

// Error codes returned by the mod's web API.
const ERROR_TEXTS: Record<string, string> = {
  INVALID_CODE: 'Neplatný nebo prošlý kód. Napiš ve hře /gts web pro nový.',
  RATE_LIMITED: 'Příliš mnoho pokusů, zkus to za chvíli.',
  UNAUTHORIZED: 'Přihlášení vypršelo, přihlas se znovu.',
  NOT_ONLINE: 'Pro nákup musíš být připojený ve hře.',
  NOT_AVAILABLE: 'Tato nabídka už není dostupná.',
  OWN_LISTING: 'Nemůžeš koupit vlastní nabídku.',
  NOT_OWNER: 'Tohle není tvoje nabídka.',
  CLAIM_FAILED: 'Vyzvednutí se nezdařilo.',
  NO_INVENTORY_SPACE: 'Nemáš místo v inventáři.',
  INSUFFICIENT_FUNDS: 'Nemáš dost peněz.',
  PURCHASE_FAILED: 'Nákup se nezdařil.',
  TRADE_NOT_FOUND: 'Inzerát nebyl nalezen.',
  BAD_REQUEST: 'Neplatný požadavek.',
  INTERNAL: 'Chyba serveru, zkus to znovu.',
};

const errorText = (code: string | undefined, fallback: string) =>
  (code && ERROR_TEXTS[code]) || fallback;

const getTimeLeft = (endTime: number) => {
  const diff = endTime - Date.now();
  if (diff <= 0) return 'vypršelo';
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
  const minutes = Math.floor((diff / 1000 / 60) % 60);

  if (days > 0) return `${days}d ${hours}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
};

export default function Home() {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [listings, setListings] = useState<any[]>([]);
  const [loginCode, setLoginCode] = useState('');
  const [error, setError] = useState('');
  const [buyMessage, setBuyMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [sortOrder, setSortOrder] = useState('newest');
  const [visits, setVisits] = useState<number | null>(null);

  const [showFilters, setShowFilters] = useState(false);
  const [filterShiny, setFilterShiny] = useState(false);
  const [filterHyperTrained, setFilterHyperTrained] = useState(false);
  const [filterMinLevel, setFilterMinLevel] = useState(1);
  const [filterMaxLevel, setFilterMaxLevel] = useState(100);

  const [activeTab, setActiveTab] = useState<'gts' | 'expired' | 'trades'>('gts');
  const [expiredListings, setExpiredListings] = useState<any[]>([]);
  const [trades, setTrades] = useState<any[]>([]);
  const [showTradeForm, setShowTradeForm] = useState(false);
  const [tradeType, setTradeType] = useState<'wtb' | 'wts'>('wtb');
  const [tradePokemon, setTradePokemon] = useState('');
  const [tradeNote, setTradeNote] = useState('');
  const [isSubmittingTrade, setIsSubmittingTrade] = useState(false);
  const [tradeSearchQuery, setTradeSearchQuery] = useState('');
  const [tradeFilterType, setTradeFilterType] = useState<'all' | 'wtb' | 'wts'>('all');
  const [tradeHistory, setTradeHistory] = useState<any[]>([]);

  useEffect(() => {
    // Login link from the in-game /gts web command: /?code=XXXX
    const params = new URLSearchParams(window.location.search);
    const codeFromUrl = params.get('code');
    if (codeFromUrl) {
      window.history.replaceState(null, '', window.location.pathname);
      loginWithCode(codeFromUrl);
    } else {
      fetchUser();
    }

    // Započítat návštěvu (pojistka proti F5 spamu přes localStorage na 1 hodinu)
    const lastVisit = localStorage.getItem('gts_last_visit');
    const now = Date.now();
    const cooldown = 1000 * 60 * 60; // 1 hodina v milisekundách

    if (!lastVisit || now - parseInt(lastVisit) > cooldown) {
      // Započítat novou návštěvu
      fetch('/api/counter', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
          if (data && typeof data.count === 'number') {
            setVisits(data.count);
            localStorage.setItem('gts_last_visit', now.toString());
          }
        })
        .catch(err => console.error('Error fetching counter', err));
    } else {
      // Jen stáhnout aktuální číslo bez započítání
      fetch('/api/counter')
        .then(res => res.json())
        .then(data => {
          if (data && typeof data.count === 'number') {
            setVisits(data.count);
          }
        })
        .catch(err => console.error('Error fetching counter', err));
    }
  }, []);

  useEffect(() => {
    if (user) {
      fetchListings();
      fetchTrades();
      fetchExpired();

      fetch('/api/trades/history')
        .then(res => res.json())
        .then(data => {
          if (Array.isArray(data)) setTradeHistory(data);
        })
        .catch(() => setTradeHistory([]));
      // Poll every 10 seconds
      const interval = setInterval(() => {
        fetchListings();
        fetchTrades();
        fetchExpired();
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [user]);

  const fetchExpired = async () => {
    try {
      const res = await fetch('/api/listings/expired');
      if (res.ok) setExpiredListings(await res.json());
    } catch (e) {
      console.error('Failed to fetch expired listings', e);
    }
  };

  const handleClaim = async (listingId: string) => {
    setBuyMessage(null);
    try {
      const res = await fetch('/api/listings/claim', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ listing_id: listingId }),
      });
      const data = await res.json();
      if (res.ok) {
        setBuyMessage({ text: 'Vyzvednuto! Najdeš to ve hře ve své partě, PC nebo inventáři.', type: 'success' });
      } else {
        setBuyMessage({ text: errorText(data.error, 'Vyzvednutí se nezdařilo'), type: 'error' });
      }
    } catch {
      setBuyMessage({ text: 'Chyba sítě', type: 'error' });
    }
    fetchExpired();
    setTimeout(() => setBuyMessage(null), 5000);
  };

  const fetchTrades = async () => {
    try {
      const res = await fetch('/api/trades');
      const data = await res.json();
      if (res.ok) setTrades(data);
    } catch (e) {
      console.error('Failed to fetch trades', e);
    }
  };

  const fetchUser = async () => {
    try {
      const res = await fetch('/api/auth/me');
      const data = await res.json();
      if (data.authenticated) {
        setUser(data.user);
      }
    } catch (e) {
      console.error('Failed to fetch user', e);
    } finally {
      setLoading(false);
    }
  };

  const fetchListings = async () => {
    setIsRefreshing(true);
    try {
      // The Next.js API route that proxies to the Mod's API
      const res = await fetch('/api/listings');
      if (res.ok) {
        const data = await res.json();
        setListings(data);
      }
    } catch (e) {
      console.error('Failed to fetch listings', e);
    } finally {
      setTimeout(() => setIsRefreshing(false), 500); // Malá pauza, ať uživatel vidí, že se to obnovilo
    }
  };

  const loginWithCode = async (code: string) => {
    setError('');
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: code.trim() }),
      });
      const data = await res.json();
      if (res.ok) {
        await fetchUser();
        return;
      }
      setError(errorText(data.error, 'Přihlášení se nezdařilo'));
    } catch {
      setError('Chyba sítě');
    }
    setLoading(false);
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    await loginWithCode(loginCode);
  };

  const handleLogout = async () => {
    await fetch('/api/auth/logout', { method: 'POST' });
    setUser(null);
  };

  const handleBuy = async (listingId: string) => {
    if (!user) return;
    setBuyMessage(null);
    try {
      const res = await fetch('/api/buy', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ listing_id: listingId }),
      });
      const data = await res.json();
      if (res.ok) {
        setBuyMessage({ text: 'Nákup proběhl úspěšně! Pokémona najdeš ve hře ve své partě nebo v PC.', type: 'success' });
        fetchListings(); // Refresh instantly
      } else {
        setBuyMessage({ text: errorText(data.error, 'Nákup se nezdařil'), type: 'error' });
      }
    } catch (e) {
      setBuyMessage({ text: 'Chyba sítě', type: 'error' });
    }

    setTimeout(() => setBuyMessage(null), 5000);
  };

  const handleCancelListing = async (listingId: string) => {
    if (!confirm('Opravdu chceš zrušit tuto nabídku? Přesune se do záložky Prošlé nabídky.')) return;
    setBuyMessage(null);
    try {
      const res = await fetch('/api/listings/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ listing_id: listingId }),
      });
      const data = await res.json();
      if (res.ok) {
        setBuyMessage({ text: 'Nabídka zrušena. Vyzvedneš ji v záložce Prošlé nabídky nebo ve hře přes /gts expired.', type: 'success' });
        fetchListings();
        fetchExpired();
      } else {
        setBuyMessage({ text: errorText(data.error, 'Zrušení se nezdařilo'), type: 'error' });
      }
    } catch {
      setBuyMessage({ text: 'Chyba sítě', type: 'error' });
    }

    setTimeout(() => setBuyMessage(null), 5000);
  };

  const handleTradeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tradePokemon) return;
    setIsSubmittingTrade(true);
    try {
      const res = await fetch('/api/trades', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: tradeType,
          pokemon: tradePokemon,
          note: tradeNote
        })
      });
      if (res.ok) {
        setShowTradeForm(false);
        setTradePokemon('');
        setTradeNote('');
        fetchTrades();
        setBuyMessage({ text: 'Inzerát byl úspěšně přidán!', type: 'success' });
        setTimeout(() => setBuyMessage(null), 5000);

        fetch('/api/trades/history', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type: tradeType,
            pokemon: tradePokemon,
            note: tradeNote
          })
        }).then(res => res.json()).then(data => {
          if (Array.isArray(data)) setTradeHistory(data);
        }).catch(err => console.error(err));
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsSubmittingTrade(false);
    }
  };

  const handleDeleteTrade = async (tradeId: string) => {
    if (!confirm('Opravdu chceš smazat tento inzerát?')) return;
    try {
      const res = await fetch('/api/trades', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: tradeId })
      });
      if (res.ok) {
        fetchTrades();
        setBuyMessage({ text: 'Inzerát byl smazán.', type: 'success' });
        setTimeout(() => setBuyMessage(null), 3000);
      } else {
        const data = await res.json();
        setBuyMessage({ text: errorText(data.error, 'Chyba při mazání.'), type: 'error' });
        setTimeout(() => setBuyMessage(null), 3000);
      }
    } catch(err) {
      console.error(err);
    }
  };

  const handleExtendTrade = async (tradeId: string) => {
    try {
      const res = await fetch('/api/trades', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: tradeId })
      });
      if (res.ok) {
        fetchTrades();
        setBuyMessage({ text: 'Inzerát prodloužen o dalších 72 hodin!', type: 'success' });
        setTimeout(() => setBuyMessage(null), 3000);
      } else {
        const data = await res.json();
        setBuyMessage({ text: errorText(data.error, 'Chyba při prodlužování.'), type: 'error' });
        setTimeout(() => setBuyMessage(null), 3000);
      }
    } catch(err) {
      console.error(err);
    }
  };

  if (loading) {
    return <div className="min-h-screen bg-slate-950 flex items-center justify-center text-white">Načítám...</div>;
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-950 flex flex-col items-center justify-center p-4 relative overflow-hidden">
        {/* Decorative background elements */}
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-blue-600/20 blur-[120px] pointer-events-none"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-purple-600/20 blur-[120px] pointer-events-none"></div>

        <div className="w-full max-w-md bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-2xl relative z-10">
          <div className="flex justify-center mb-6">
            <div className="w-20 h-20 flex items-center justify-center">
              <img src="https://navody.pokeland.cz/cobblemon/cobblemon_gengar.webp" alt="Pokeland Logo" className="w-full h-full object-contain drop-shadow-[0_0_15px_rgba(168,85,247,0.6)]" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-center text-white mb-2 tracking-tight">Pokeland GTS</h1>
          <p className="text-center text-slate-400 mb-8 font-medium">Napiš ve hře <span className="text-white font-mono">/gts web</span> a klikni na odkaz, nebo sem opiš kód.</p>

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-base font-semibold text-slate-300 mb-2">Kód ze hry</label>
              <input
                type="text"
                value={loginCode}
                onChange={(e) => setLoginCode(e.target.value.toUpperCase())}
                className="w-full bg-slate-900/50 border border-slate-700 rounded-xl px-4 py-3 text-white text-center font-mono text-2xl tracking-[0.3em] focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all placeholder:text-slate-500"
                placeholder="XXXXXXXX"
                autoComplete="one-time-code"
                maxLength={16}
                required
              />
            </div>
            {error && <p className="text-red-400 text-base font-medium text-center bg-red-400/10 py-2 rounded-lg">{error}</p>}
            <button
              type="submit"
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white font-bold py-3 px-4 rounded-xl shadow-lg transition-all transform hover:scale-[1.02] active:scale-95 flex items-center justify-center gap-2"
            >
              <LogIn className="w-5 h-5" />
              Přihlásit se
            </button>
          </form>
        </div>
      </div>
    );
  }

  const filteredListings = listings.filter(l => {
    // Schovat expirované nabídky (endTime je v minulosti)
    if (l.endTime && l.endTime < Date.now()) {
      return false;
    }

    const pkm = l.pokemon;
    if (!pkm) return false;

    if (filterShiny && !pkm.Shiny) return false;
    if (filterHyperTrained) {
      if (!pkm.IVs?.HyperTrained || Object.keys(pkm.IVs.HyperTrained).length === 0) {
        return false;
      }
    }
    if (pkm.Level < filterMinLevel || pkm.Level > filterMaxLevel) return false;

    const species = (pkm.Species || '').replace('cobblemon:', '').toLowerCase();
    return species.includes(searchQuery.toLowerCase());
  }).sort((a, b) => {
    if (sortOrder === 'price_asc') return a.price - b.price;
    if (sortOrder === 'price_desc') return b.price - a.price;

    const speciesA = (a.pokemon?.Species || '').replace('cobblemon:', '');
    const speciesB = (b.pokemon?.Species || '').replace('cobblemon:', '');

    if (sortOrder === 'alpha_asc') return speciesA.localeCompare(speciesB);
    if (sortOrder === 'alpha_desc') return speciesB.localeCompare(speciesA);

    // Default 'newest' (highest endTime first)
    return (b.endTime || 0) - (a.endTime || 0);
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-200 font-sans selection:bg-blue-500/30">
      <nav className="sticky top-0 z-50 backdrop-blur-md bg-slate-950/80 border-b border-white/10 px-6 py-4 flex justify-between items-center shadow-lg">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 flex items-center justify-center">
            <img src="https://navody.pokeland.cz/cobblemon/cobblemon_gengar.webp" alt="Pokeland Logo" className="w-full h-full object-contain drop-shadow-[0_0_8px_rgba(168,85,247,0.5)]" />
          </div>
          <h1 className="text-2xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400 tracking-tight">Pokeland GTS</h1>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-base font-medium bg-slate-800/50 px-4 py-2 rounded-full border border-white/5 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
            Přihlášen jako <span className="text-white font-bold">{user.realname}</span>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-base font-semibold hover:bg-white/5 px-3 py-2 rounded-lg"
          >
            <LogOut className="w-4 h-4" /> Odhlásit se
          </button>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-6 py-10 relative">
        {/* Alert Notifications */}
        {buyMessage && (
          <div className={`fixed top-24 right-6 z-50 p-4 rounded-xl shadow-2xl backdrop-blur-md border animate-in slide-in-from-right flex items-center gap-3 ${buyMessage.type === 'success' ? 'bg-green-900/30 border-green-500/30 text-green-300' : 'bg-red-900/30 border-red-500/30 text-red-300'
            }`}>
            {buyMessage.type === 'success' ? <CheckCircle2 className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
            <p className="font-semibold">{buyMessage.text}</p>
          </div>
        )}

        {/* Tabs */}
        <div className="flex flex-col md:flex-row gap-4 mb-8 border-b border-white/10 pb-6">
          <button
            onClick={() => setActiveTab('gts')}
            className={`flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-lg transition-all ${activeTab === 'gts' ? 'bg-blue-600 text-white shadow-[0_0_20px_rgba(37,99,235,0.4)]' : 'bg-slate-900/50 text-slate-400 hover:text-white hover:bg-slate-800'}`}
          >
            <Store className="w-5 h-5" /> GTS
          </button>
          <button
            onClick={() => { setActiveTab('expired'); fetchExpired(); }}
            className={`flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-lg transition-all ${activeTab === 'expired' ? 'bg-blue-600 text-white shadow-[0_0_20px_rgba(37,99,235,0.4)]' : 'bg-slate-900/50 text-slate-400 hover:text-white hover:bg-slate-800'}`}
          >
            <Archive className="w-5 h-5" /> Prošlé nabídky
            {expiredListings.length > 0 && (
              <span className="ml-1 min-w-6 h-6 px-1.5 rounded-full bg-amber-500 text-slate-950 text-sm flex items-center justify-center">{expiredListings.length}</span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('trades')}
            className={`flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-lg transition-all ${activeTab === 'trades' ? 'bg-blue-600 text-white shadow-[0_0_20px_rgba(37,99,235,0.4)]' : 'bg-slate-900/50 text-slate-400 hover:text-white hover:bg-slate-800'}`}
          >
            <MessageSquare className="w-5 h-5" /> Inzerce hráčů
          </button>
        </div>

        {activeTab === 'expired' ? (
          <div>
            <div className="mb-10">
              <h2 className="text-4xl font-bold text-white mb-2">Prošlé nabídky</h2>
              <p className="text-slate-400">Tvoje nabídky, které vypršely nebo jsi je zrušil. Pro vyzvednutí musíš být připojený ve hře.</p>
            </div>
            {expiredListings.length === 0 ? (
              <div className="text-center py-20 bg-slate-900/30 rounded-3xl border border-white/5">
                <Archive className="w-16 h-16 text-slate-600 mx-auto mb-4" />
                <h3 className="text-2xl font-bold text-slate-300 mb-2">Nic k vyzvednutí</h3>
                <p className="text-slate-400">Všechny tvoje nabídky jsou aktivní nebo prodané.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {expiredListings.map((listing) => {
                  const pkm = listing.pokemon;
                  const info = pkm ? describePokemon(pkm) : null;
                  return (
                    <div key={listing.id} className="bg-slate-900/50 border border-white/10 rounded-2xl p-5 flex flex-col gap-4">
                      <div className="flex items-center gap-4">
                        <div className="w-16 h-16 bg-slate-950 rounded-full border border-white/5 flex flex-shrink-0 items-center justify-center overflow-hidden">
                          {info ? (
                            <img
                              src={spriteUrl(info.spriteName, !!pkm.Shiny)}
                              alt={info.speciesName}
                              className="w-20 h-20 object-contain pixelated"
                              onError={(e) => { (e.target as HTMLImageElement).src = FALLBACK_SPRITE; }}
                            />
                          ) : (
                            <Package className="w-8 h-8 text-slate-400" />
                          )}
                        </div>
                        <div className="min-w-0">
                          <h3 className="text-lg font-bold text-white capitalize truncate">
                            {info ? info.speciesName : listing.listingName} {pkm?.Shiny && '★'}
                          </h3>
                          {pkm && <p className="text-slate-400">Úroveň {pkm.Level}</p>}
                          {info && info.displayFeatures.length > 0 && (
                            <p className="text-slate-500 text-sm capitalize truncate">{info.displayFeatures.join(', ')}</p>
                          )}
                        </div>
                      </div>
                      <p className="text-slate-400">Cena: <span className="text-white font-bold">${listing.price.toLocaleString()}</span></p>
                      <button
                        onClick={() => handleClaim(listing.id)}
                        className="w-full font-bold py-3 px-4 rounded-xl transition-all shadow-md flex items-center justify-center gap-2 bg-amber-500 hover:bg-amber-400 text-slate-950 active:scale-95"
                      >
                        <PackageOpen className="w-5 h-5" /> Vyzvednout
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ) : activeTab === 'gts' ? (
          <>
            <div className="flex flex-col md:flex-row justify-between items-end mb-10 gap-6">
              <div>
                <h2 className="text-4xl font-bold text-white mb-2">Dostupní Pokémoni</h2>
            <p className="text-slate-400 mb-3">Objevuj a kupuj Pokémony, které nabízejí ostatní hráči.</p>
            <div className="inline-flex items-center gap-2 bg-yellow-900/20 border border-yellow-500/30 text-yellow-400 undefined md:text-sm px-3 py-1.5 rounded-lg font-medium shadow-sm">
              <Sparkles className="w-3.5 h-3.5" />
              <span>IVs se zlatým rámečkem jsou <strong className="font-bold">Hypertrained</strong>. Najetím myší zobrazíš původní hodnotu.</span>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            <div className="relative flex-grow md:w-64 group">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500 group-focus-within:text-blue-400 transition-colors" />
              <input
                type="text"
                placeholder="Hledej druh..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-900/80 border border-slate-700 rounded-xl pl-10 pr-4 py-3 text-white focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all"
              />
            </div>

            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className="bg-slate-900/80 border border-slate-700 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all cursor-pointer font-medium"
            >
              <option value="newest">Nejnovější</option>
              <option value="price_asc">Od nejlevnějšího</option>
              <option value="price_desc">Od nejdražšího</option>
              <option value="alpha_asc">Od A do Z</option>
              <option value="alpha_desc">Od Z do A</option>
            </select>

            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`p-3 rounded-xl border transition-colors shadow-md flex items-center justify-center shrink-0 ${showFilters ? 'bg-blue-600 border-blue-500 text-white' : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-600'}`}
              title="Pokročilé filtry"
            >
              <SlidersHorizontal className="w-5 h-5" />
            </button>
            <button
              onClick={fetchListings}
              className="bg-slate-800 hover:bg-slate-700 text-white p-3 rounded-xl border border-slate-600 transition-colors shadow-md flex items-center justify-center shrink-0"
              title="Obnovit nabídky"
            >
              <RefreshCw className={`w-5 h-5 ${isRefreshing ? 'animate-spin text-blue-400' : 'text-slate-300'}`} />
            </button>
          </div>
        </div>

        {showFilters && (
          <div className="bg-slate-900/80 border border-slate-700 rounded-xl p-5 mb-10 flex flex-wrap gap-6 items-center animate-in slide-in-from-top-2">
            <label className="flex items-center gap-2 cursor-pointer group">
              <input type="checkbox" checked={filterShiny} onChange={e => setFilterShiny(e.target.checked)} className="w-5 h-5 rounded border-slate-600 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900" />
              <span className="text-slate-300 font-medium group-hover:text-white transition-colors">Pouze Shiny ✨</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer group">
              <input type="checkbox" checked={filterHyperTrained} onChange={e => setFilterHyperTrained(e.target.checked)} className="w-5 h-5 rounded border-slate-600 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900" />
              <span className="text-slate-300 font-medium group-hover:text-white transition-colors">Pouze HyperTrained 🥇</span>
            </label>
            <div className="flex items-center gap-3">
              <span className="text-slate-400 font-medium">Level:</span>
              <input type="number" min="1" max="100" value={filterMinLevel} onChange={e => setFilterMinLevel(Number(e.target.value) || 1)} className="w-20 bg-slate-800 border border-slate-600 rounded-lg px-3 py-1.5 text-white focus:outline-none focus:border-blue-500 text-center" />
              <span className="text-slate-500">-</span>
              <input type="number" min="1" max="100" value={filterMaxLevel} onChange={e => setFilterMaxLevel(Number(e.target.value) || 100)} className="w-20 bg-slate-800 border border-slate-600 rounded-lg px-3 py-1.5 text-white focus:outline-none focus:border-blue-500 text-center" />
            </div>
          </div>
        )}

        {listings.length === 0 ? (
          <div className="py-20 text-center bg-slate-900/30 rounded-3xl border border-white/5 backdrop-blur-sm">
            <div className="mx-auto w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center mb-4">
              <Sparkles className="w-8 h-8 text-slate-500" />
            </div>
            <h3 className="text-2xl font-bold text-white mb-2">Žádné aktivní nabídky</h3>
            <p className="text-slate-400">Zkus to později, až hráči přidají nové Pokémony!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredListings.map((listing) => {
              const pkm = listing.pokemon || {};
              const isShiny = pkm.Shiny;
              const isOwner = listing.sellerUuid === user.id;

              const speciesName = (pkm.Species || 'Neznámý').replace('cobblemon:', '');
              const baseNature = (pkm.Nature || 'Neznámý').replace('cobblemon:', '');
              const mintedNature = pkm.MintedNature ? pkm.MintedNature.replace('cobblemon:', '') : null;
              const natureName = mintedNature ? `${mintedNature} (${baseNature})` : baseNature;
              const abilityName = formatCobblemonString(pkm.Ability?.AbilityName || 'Neznámá');

              const { displayFeatures, spriteName: pokeApiSpecies } = describePokemon(pkm);

              // New stats
              const teraType = (pkm.TeraType || 'normal').replace('cobblemon:', '');
              const caughtBall = (pkm.CaughtBall || 'poke_ball').replace('cobblemon:', '').replace('_ball', ' Ball').replace(/_/g, ' ');
              const friendship = pkm.Friendship ?? 0;
              const breedableVal = pkm.PersistentData?.breedable;
              const breedableText = breedableVal === 0 ? 'Nelze' : (breedableVal > 0 ? `Ano (${breedableVal}x)` : 'Lze');
              const moves = (pkm.MoveSet || []).map((m: any) => m.MoveName).filter(Boolean);

              return (
                <div key={listing.id} className="group relative bg-slate-900/50 hover:bg-slate-800/80 border border-white/10 hover:border-blue-500/30 rounded-2xl overflow-hidden transition-all duration-300 hover:shadow-[0_0_30px_rgba(59,130,246,0.15)] flex flex-col">
                  {/* Card Header */}
                  <div className="px-5 py-4 border-b border-white/5 bg-slate-900/80">
                    <div className="flex items-center gap-4">
                      <div className="w-20 h-20 bg-slate-950 rounded-full shadow-inner border border-white/5 flex flex-shrink-0 items-center justify-center relative overflow-hidden group-hover:scale-110 transition-transform">
                        <img
                          src={spriteUrl(pokeApiSpecies, isShiny)}
                          alt={speciesName}
                          className="w-24 h-24 object-contain pixelated drop-shadow-md scale-125"
                          onError={(e) => { (e.target as HTMLImageElement).src = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png'; }}
                        />
                      </div>
                      <div>
                        <div className="flex flex-wrap items-center gap-2 mb-1.5">
                          <h3 className={`text-2xl font-bold capitalize truncate ${isShiny ? 'text-yellow-400 drop-shadow-[0_0_10px_rgba(250,204,21,0.5)]' : 'text-blue-300'}`}>
                            {speciesName} {isShiny && '★'}
                          </h3>
                          <span className="text-base font-bold px-2.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-white/10">Lv. {pkm.Level || 1}</span>
                        </div>
                        {displayFeatures.length > 0 && (
                          <div className="flex flex-wrap gap-1.5 mb-1.5">
                            {displayFeatures.map(feat => (
                              <span key={feat} className="text-sm font-bold px-2 py-0.5 rounded bg-purple-900/40 text-purple-300 border border-purple-500/30 capitalize">{feat}</span>
                            ))}
                          </div>
                        )}
                        <p className="text-base text-slate-500 font-medium">Prodává <span className="text-slate-300">{listing.sellerName}</span></p>
                      </div>
                    </div>
                  </div>

                  {/* Card Body */}
                  <div className="p-5 flex-grow space-y-3">
                    {/* Základní info */}
                    <div className="grid grid-cols-2 gap-3 text-base">
                      <div className="bg-slate-950/50 p-2.5 rounded-xl border border-white/5">
                        <span className="block text-slate-500 text-xs mb-0.5 uppercase tracking-wider font-bold">Povaha</span>
                        <span className="text-slate-200 capitalize font-medium">{natureName}</span>
                      </div>
                      <div className="bg-slate-950/50 p-2.5 rounded-xl border border-white/5">
                        <span className="block text-slate-500 text-xs mb-0.5 uppercase tracking-wider font-bold">Schopnost</span>
                        <span className="text-slate-200 capitalize font-medium">{abilityName}</span>
                      </div>
                    </div>

                    {/* Doplňkové info z JSONu */}
                    <div className="grid grid-cols-2 gap-2 text-base">
                      <div className="bg-slate-950/50 p-2 rounded-lg border border-white/5 flex flex-col items-center justify-center text-center">
                        <span className="text-slate-500 text-xs uppercase tracking-wider font-bold mb-1">Tera Typ</span>
                        <span className="text-blue-300 capitalize text-sm font-bold">{teraType}</span>
                      </div>
                      <div className="bg-slate-950/50 p-2 rounded-lg border border-white/5 flex flex-col items-center justify-center text-center">
                        <span className="text-slate-500 text-xs uppercase tracking-wider font-bold mb-1">Pokéball</span>
                        <span className="text-red-300 capitalize text-sm font-bold">{caughtBall}</span>
                      </div>
                      <div className="bg-slate-950/50 p-2 rounded-lg border border-white/5 flex flex-col items-center justify-center text-center">
                        <span className="text-slate-500 text-xs uppercase tracking-wider font-bold mb-1">Friendship</span>
                        <span className="text-pink-300 text-sm font-bold">{friendship}</span>
                      </div>
                      <div className="bg-slate-950/50 p-2 rounded-lg border border-white/5 flex flex-col items-center justify-center text-center">
                        <span className="text-slate-500 text-xs uppercase tracking-wider font-bold mb-1">Breeding</span>
                        <span className="text-green-300 text-sm font-bold">{breedableText}</span>
                      </div>
                    </div>

                    {/* Útoky (Moves) */}
                    <div className="bg-slate-950/50 p-2.5 rounded-xl border border-white/5">
                      <span className="block text-slate-500 text-xs mb-1.5 uppercase tracking-wider font-bold">Útoky (MoveSet)</span>
                      <div className="grid grid-cols-2 gap-1.5">
                        {moves.length > 0 ? moves.map((move: string, idx: number) => (
                          <div key={idx} className="bg-slate-900 border border-white/5 rounded px-2 py-1 text-sm text-slate-300 capitalize text-center font-medium truncate">
                            {formatCobblemonString(move)}
                          </div>
                        )) : (
                          <div className="col-span-2 text-sm text-slate-600 italic text-center">Žádné útoky</div>
                        )}
                      </div>
                    </div>

                    {/* IVs */}
                    <div className="bg-slate-950/50 p-2.5 rounded-xl border border-white/5">
                      <span className="block text-slate-500 text-xs mb-1.5 uppercase tracking-wider font-bold">IVs (Genetika)</span>
                      <div className="grid grid-cols-6 gap-1 text-center">
                        {[
                          { key: 'hp', label: 'HP' },
                          { key: 'attack', label: 'ATK' },
                          { key: 'defence', label: 'DEF' },
                          { key: 'special_attack', label: 'SPA' },
                          { key: 'special_defence', label: 'SPD' },
                          { key: 'speed', label: 'SPE' }
                        ].map(stat => {
                          const baseVal = (pkm.IVs?.Base || {})[`cobblemon:${stat.key}`] ?? 0;
                          const htVal = pkm.IVs?.HyperTrained ? pkm.IVs.HyperTrained[`cobblemon:${stat.key}`] : undefined;

                          const isHyperTrained = htVal !== undefined;
                          const val = isHyperTrained ? htVal : baseVal;

                          const isPerfect = val === 31 && !isHyperTrained;
                          const isZero = val === 0;

                          let bgClass = 'bg-slate-900 border-white/5';
                          let textClass = 'text-slate-300 font-medium';
                          let displayVal: string | number = val;

                          if (isHyperTrained) {
                            bgClass = 'bg-yellow-900/20 border-yellow-500/30';
                            textClass = 'text-yellow-400 font-bold';
                          } else if (isPerfect) {
                            bgClass = 'bg-green-900/20 border-green-500/30';
                            textClass = 'text-green-400 font-bold';
                          } else if (isZero) {
                            bgClass = 'bg-red-900/20 border-red-500/30';
                            textClass = 'text-red-400 font-bold';
                          }

                          return (
                            <div key={stat.key} className={`flex flex-col rounded p-1 border ${bgClass}`} title={isHyperTrained ? `Původní IV: ${baseVal}` : undefined}>
                              <span className="text-slate-500 text-xs font-bold">{stat.label}</span>
                              <span className={`text-sm ${textClass}`}>{displayVal}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    {/* EVs */}
                    <div className="bg-slate-950/50 p-2.5 rounded-xl border border-white/5">
                      <span className="block text-slate-500 text-xs mb-1.5 uppercase tracking-wider font-bold">EVs (Trénink)</span>
                      <div className="grid grid-cols-6 gap-1 text-center">
                        {[
                          { key: 'hp', label: 'HP' },
                          { key: 'attack', label: 'ATK' },
                          { key: 'defence', label: 'DEF' },
                          { key: 'special_attack', label: 'SPA' },
                          { key: 'special_defence', label: 'SPD' },
                          { key: 'speed', label: 'SPE' }
                        ].map(stat => {
                          const val = (pkm.EVs || {})[`cobblemon:${stat.key}`] ?? 0;
                          const isMax = val >= 252;
                          return (
                            <div key={stat.key} className={`flex flex-col rounded p-1 border ${isMax ? 'bg-blue-900/20 border-blue-500/30' : 'bg-slate-900 border-white/5'}`}>
                              <span className="text-slate-500 text-xs font-bold">{stat.label}</span>
                              <span className={`text-sm ${isMax ? 'text-blue-400 font-bold' : 'text-slate-400 font-medium'}`}>{val}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>

                  {/* Card Footer */}
                  <div className="p-5 pt-0 mt-auto">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <Coins className="w-5 h-5 text-yellow-400" />
                        <span className="text-2xl font-bold text-white">${listing.price.toLocaleString()}</span>
                      </div>
                      {listing.endTime && (
                        <div className="text-right" title={`Končí: ${new Date(listing.endTime).toLocaleString('cs-CZ')}`}>
                          <span className="text-sm text-slate-400 font-medium">⏳ Za {getTimeLeft(listing.endTime)}</span>
                        </div>
                      )}
                    </div>
                    <button
                      onClick={() => isOwner ? handleCancelListing(listing.id) : handleBuy(listing.id)}
                      className={`w-full font-bold py-3 px-4 rounded-xl transition-all shadow-md flex items-center justify-center gap-2 active:scale-95 ${isOwner
                        ? 'bg-slate-800 hover:bg-red-600/80 text-slate-300 hover:text-white'
                        : 'bg-blue-600 hover:bg-blue-500 text-white hover:shadow-[0_0_20px_rgba(59,130,246,0.4)]'
                        }`}
                    >
                      {isOwner ? <><Trash2 className="w-4 h-4" /> Zrušit nabídku</> : 'Koupit Pokémona'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
          </>
        ) : (
          <div>
            <div className="flex flex-col md:flex-row justify-between items-center mb-10 gap-4">
              <div>
                <h2 className="text-4xl font-bold text-white mb-2">Nástěnka Inzerátů</h2>
                <p className="text-slate-400">Co ostatní hráči shánějí nebo nabízejí k výměně. <span className="text-slate-500 text-sm ml-2">(Inzeráty se mažou automaticky po 72 hodinách)</span></p>
              </div>
              <button 
                onClick={() => setShowTradeForm(!showTradeForm)}
                className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-3 rounded-xl font-bold transition-all shadow-md active:scale-95 shrink-0"
              >
                + Přidat inzerát
              </button>
            </div>

            {showTradeForm && (
              <div className="bg-slate-900/80 border border-slate-700 rounded-xl p-6 mb-10 animate-in slide-in-from-top-2">
                <form onSubmit={handleTradeSubmit} className="space-y-4">
                  {tradeHistory.length > 0 && (
                    <div className="mb-4 bg-slate-950/50 p-3 rounded-xl border border-white/5">
                      <label className="block text-slate-400 font-bold mb-2 text-xs uppercase tracking-wider">Historie tvých inzerátů (klikni pro vyplnění)</label>
                      <div className="flex flex-wrap gap-2">
                        {tradeHistory.map((h, i) => (
                          <button 
                            key={i} type="button" 
                            onClick={() => { setTradeType(h.type); setTradePokemon(h.pokemon); setTradeNote(h.note || ''); }}
                            className="text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 px-3 py-1.5 rounded-lg border border-white/10 flex items-center gap-1.5 transition-colors"
                          >
                            <span className={`font-black uppercase ${h.type === 'wtb' ? 'text-red-400' : 'text-green-400'}`}>{h.type}</span>
                            <span className="capitalize">{h.pokemon}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="flex flex-col md:flex-row gap-4">
                    <div className="w-full md:w-1/4">
                      <label className="block text-slate-300 font-bold mb-2">Typ inzerátu</label>
                      <select value={tradeType} onChange={e => setTradeType(e.target.value as 'wtb'|'wts')} className="w-full bg-slate-800 border border-slate-600 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-blue-500 font-bold">
                        <option value="wtb">Hledám (WTB)</option>
                        <option value="wts">Nabízím (WTS)</option>
                      </select>
                    </div>
                    <div className="w-full md:w-3/4">
                      <label className="block text-slate-300 font-bold mb-2">Jméno Pokémona (anglicky)</label>
                      <input type="text" required value={tradePokemon} onChange={e => setTradePokemon(e.target.value)} placeholder="např. Charizard" className="w-full bg-slate-800 border border-slate-600 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-blue-500" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-slate-300 font-bold mb-2">Poznámka (volitelné)</label>
                    <input type="text" value={tradeNote} onChange={e => setTradeNote(e.target.value)} placeholder="např. Dám za to Master Ball..." className="w-full bg-slate-800 border border-slate-600 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-blue-500" />
                  </div>
                  <div className="flex justify-end pt-2">
                    <button type="submit" disabled={isSubmittingTrade} className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white font-bold py-3 px-8 rounded-xl transition-all shadow-lg active:scale-95">
                      {isSubmittingTrade ? 'Odesílání...' : 'Zveřejnit inzerát'}
                    </button>
                  </div>
                </form>
              </div>
            )}


            {/* Filter and Search Bar for Trades */}
            <div className="bg-slate-900/50 p-4 rounded-2xl border border-white/5 flex flex-col md:flex-row items-center justify-between gap-4 mb-8">
              <div className="flex bg-slate-950 p-1 rounded-xl">
                <button onClick={() => setTradeFilterType('all')} className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${tradeFilterType === 'all' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'}`}>Vše</button>
                <button onClick={() => setTradeFilterType('wtb')} className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${tradeFilterType === 'wtb' ? 'bg-red-600 text-white' : 'text-slate-400 hover:text-white'}`}>Hledám (WTB)</button>
                <button onClick={() => setTradeFilterType('wts')} className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${tradeFilterType === 'wts' ? 'bg-green-600 text-white' : 'text-slate-400 hover:text-white'}`}>Nabízím (WTS)</button>
              </div>
              <div className="relative w-full md:w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                <input 
                  type="text" 
                  value={tradeSearchQuery} 
                  onChange={e => setTradeSearchQuery(e.target.value)} 
                  placeholder="Hledat (Pokémon nebo hráč)..." 
                  className="w-full bg-slate-950 border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-500 transition-colors"
                />
              </div>
            </div>

            {(() => {
              const filteredTrades = trades.filter((t: any) => {
                if (tradeFilterType !== 'all' && t.type !== tradeFilterType) return false;
                if (tradeSearchQuery) {
                  const query = tradeSearchQuery.toLowerCase();
                  const pokemonMatch = t.pokemon.toLowerCase().includes(query);
                  const authorMatch = t.author.toLowerCase().includes(query);
                  if (!pokemonMatch && !authorMatch) return false;
                }
                return true;
              });

              if (filteredTrades.length === 0) {
                return (
                  <div className="py-20 text-center bg-slate-900/30 rounded-3xl border border-white/5 backdrop-blur-sm">
                    <div className="mx-auto w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center mb-4">
                      <MessageSquare className="w-8 h-8 text-slate-500" />
                    </div>
                    <h3 className="text-xl font-bold text-white mb-2">Žádné inzeráty</h3>
                    <p className="text-slate-400">Tvému hledání neodpovídá žádný inzerát.</p>
                  </div>
                );
              }

              return (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {filteredTrades.map((trade: any) => {
                    const isNew = trade.timestamp ? (Date.now() - trade.timestamp < 2 * 60 * 60 * 1000) : false;
                    const isAuthor = user?.realname === trade.author;

                    return (
                      <div key={trade.id} className="bg-slate-900/50 border border-white/10 rounded-2xl overflow-hidden flex flex-col relative group hover:border-white/20 transition-colors">
                        <div className={`px-4 py-2 font-black text-sm tracking-widest text-center uppercase flex items-center justify-center gap-2 ${trade.type === 'wtb' ? 'bg-red-900/80 text-red-300 border-b border-red-500/30' : 'bg-green-900/80 text-green-300 border-b border-green-500/30'}`}>
                          {trade.type === 'wtb' ? 'Hledá se (WTB)' : 'Nabízí se (WTS)'}
                          {isNew && <span className="bg-yellow-500 text-black text-[10px] px-1.5 py-0.5 rounded-sm animate-pulse">NOVÉ</span>}
                        </div>
                        <div className="p-5 flex flex-col items-center flex-grow">
                          <div className="w-24 h-24 bg-slate-950 rounded-full border border-white/5 mb-4 flex items-center justify-center p-2 relative overflow-hidden group-hover:scale-110 transition-transform">
                            <img 
                              src={`https://play.pokemonshowdown.com/sprites/gen5/${trade.pokemon.toLowerCase().trim().replace(/[^a-z0-9]/g, '')}.png`} 
                              alt={trade.pokemon} 
                              className="w-full h-full object-contain pixelated scale-125 drop-shadow-md"
                              onError={(e) => { (e.target as HTMLImageElement).src = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png'; }}
                            />
                          </div>
                          <h3 className="text-2xl font-bold text-white capitalize mb-2 text-center">{trade.pokemon}</h3>
                          {trade.note && (
                            <p className="text-slate-400 text-sm text-center bg-slate-950/50 p-3 rounded-lg border border-white/5 w-full mb-4 break-words">
                              "{trade.note}"
                            </p>
                          )}
                          <div className="mt-auto pt-4 border-t border-white/5 w-full flex items-center justify-between">
                            <span className="text-sm text-slate-500">Od: <strong className="text-slate-300">{trade.author}</strong></span>
                            <div className="flex gap-2">
                              {isAuthor && (
                                <>
                                  <button 
                                    onClick={() => handleExtendTrade(trade.id)}
                                    className="text-xs font-bold bg-blue-900/50 hover:bg-blue-600 text-blue-300 hover:text-white px-2 py-1.5 rounded-lg border border-blue-500/30 transition-colors flex items-center justify-center"
                                    title="Prodloužit inzerát o 72 hodin"
                                  >
                                    <RefreshCw className="w-3.5 h-3.5" />
                                  </button>
                                  <button 
                                    onClick={() => handleDeleteTrade(trade.id)}
                                    className="text-xs font-bold bg-red-900/50 hover:bg-red-600 text-red-300 hover:text-white px-2 py-1.5 rounded-lg border border-red-500/30 transition-colors flex items-center justify-center"
                                    title="Smazat inzerát"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                </>
                              )}
                              <button 
                                onClick={() => { navigator.clipboard.writeText(trade.author); setBuyMessage({text: 'Jméno hráče zkopírováno!', type: 'success'}); setTimeout(()=>setBuyMessage(null), 3000); }}
                                className="text-xs font-bold bg-slate-800 hover:bg-slate-700 text-white px-3 py-1.5 rounded-lg border border-white/10 transition-colors"
                                title="Zkopírovat Nick"
                              >
                                📋 Nick
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })()}
          </div>
        )}
      </main>

      <footer className="mt-20 py-8 border-t border-white/10 bg-slate-900/50 text-center">
        <p className="text-slate-500 text-base">
          Pokeland GTS Web © {new Date().getFullYear()}
        </p>
        {visits !== null && (
          <p className="text-slate-600 text-sm mt-2 flex items-center justify-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-blue-500/50 inline-block"></span>
            Celkem návštěv: <strong className="text-slate-400">{visits}</strong>
          </p>
        )}
      </footer>
    </div>
  );
}