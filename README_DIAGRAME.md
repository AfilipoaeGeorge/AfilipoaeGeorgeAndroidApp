
Actori

    - Utilizator: Actorul principal care interacționează cu sistemul

Use Case-uri Principale

    Autentificare

    - Login
    - Register
    - Autentificare (use case inclus)

    Calibrare

    - Calibrare Baseline
    - Pauză Calibrare (extinde Calibrare)
    - Reluare Calibrare (extinde Calibrare)
    - Oprire Calibrare

    Sesiuni Focus

    - Start Sesiune Focus
    - Pauză Sesiune (extinde Start Sesiune)
    - Reluare Sesiune (extinde Start Sesiune)
    - Oprire Sesiune
    - Salvare Sesiune (inclus în Oprire Sesiune)
    - Vizualizare Alerte (extinde Start Sesiune)

    Istoric

    - Vizualizare Istoric
    - Vizualizare Detalii Sesiune (extinde Vizualizare Istoric)

    Setări

    - Vizualizare Setări
    - Configurare Monitorizare Cameră (inclus în Vizualizare Setări)
    - Configurare Alerte (inclus în Vizualizare Setări)
    - Ștergere Date
    - Vizualizare Politică Confidențialitate (inclus în Vizualizare Setări)
    - Vizualizare Ajutor (inclus în Vizualizare Setări)

    Cont

    - Logout

Relații

    Include (<<include>>)

    - Login include Autentificare
    - Register include Autentificare
    - Oprire Sesiune include Salvare Sesiune
    - Oprire Calibrare include Salvare Sesiune
    - Vizualizare Setări include toate sub-use case-urile de configurare

    Extend (<<extend>>)

    - Vizualizare Alerte extinde Start Sesiune Focus
    - Pauză Sesiune extinde Start Sesiune Focus
    - Reluare Sesiune extinde Start Sesiune Focus
    - Pauză Calibrare extinde Calibrare Baseline
    - Reluare Calibrare extinde Calibrare Baseline
    - Vizualizare Detalii Sesiune extinde Vizualizare Istoric
