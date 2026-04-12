-- V22: Actualitza el contingut dels documents legals amb text real (RGPD + LOPD-GDD)

UPDATE legal_documents SET content = $PRIVACY$
<h2>1. Responsable del tractament</h2>
<p>TravelAI (en endavant, "nosaltres") és responsable del tractament de les teves dades personals.
Contacte: <a href="mailto:privacidad@travelai.local">privacidad@travelai.local</a></p>

<h2>2. Dades que recollim</h2>
<ul>
  <li><strong>Dades de compte:</strong> nom, correu electrònic i contrasenya (encriptada).</li>
  <li><strong>Dades de viatge:</strong> viatges, itineraris i preferències que tu crees.</li>
  <li><strong>Dades d'ús:</strong> data de registre i accions rellevants (auditoria de seguretat).</li>
</ul>
<p>No recollim dades sensibles (salut, origen racial, etc.) ni dades de menors de 14 anys.</p>

<h2>3. Finalitat i base legal</h2>
<ul>
  <li>Prestar el servei TravelAI (base: execució del contracte).</li>
  <li>Complir obligacions legals (RGPD, LOPD-GDD).</li>
  <li>Seguretat i prevenció de frau (interès legítim).</li>
</ul>

<h2>4. IA local i privacitat</h2>
<p>Els itineraris es generen amb Ollama, un model d'IA que s'executa <strong>localment al nostre servidor</strong>.
Les teves dades no s'envien a cap servei d'IA extern.</p>

<h2>5. Conservació de dades</h2>
<p>Conservem les dades mentre el compte estigui actiu. Després d'una sol·licitud d'esborrat,
les dades s'eliminen definitivament en 30 dies. Els registres d'auditoria es conserven 365 dies per obligació legal.</p>

<h2>6. Drets RGPD</h2>
<p>Pots exercir els teus drets d'accés, rectificació, supressió, portabilitat i limitació a través de
<a href="/my-data">Les meves dades</a> o contactant amb <a href="mailto:privacidad@travelai.local">privacidad@travelai.local</a>.
Tens dret a reclamar davant l'Agència Espanyola de Protecció de Dades (aepd.es).</p>

<h2>7. Menors</h2>
<p>El servei requereix tenir <strong>14 anys o més</strong> (LOPD-GDD Art. 7). Si detectem un compte de menor, el cancel·larem.</p>

<h2>8. Canvis a la política</h2>
<p>Notificarem canvis significatius per correu electrònic o mitjançant un avís destacat a l'aplicació.</p>
$PRIVACY$ WHERE type = 'PRIVACY_POLICY';

UPDATE legal_documents SET content = $TERMS$
<h2>1. Acceptació</h2>
<p>En registrar-te a TravelAI acceptes aquests Termes d'Ús. Si no els acceptes, no pots usar el servei.</p>

<h2>2. Descripció del servei</h2>
<p>TravelAI és una plataforma per crear i compartir itineraris de viatge amb suport d'intel·ligència artificial local.
El servei es presta "tal com és" i pot evolucionar amb el temps.</p>

<h2>3. Compte d'usuari</h2>
<ul>
  <li>Has de tenir <strong>14 anys o més</strong>.</li>
  <li>Ets responsable de mantenir la contrasenya segura.</li>
  <li>No pots crear múltiples comptes per evadir restriccions.</li>
</ul>

<h2>4. Contingut</h2>
<p>El contingut que publiques (viatges, descripcions) és de la teva propietat. En publicar-lo, ens concedeixeixes
una llicència no exclusiva per mostrar-lo a altres usuaris de la plataforma.</p>
<p>Queda prohibit publicar contingut il·legal, ofensiu, difamatori o que infringeixi drets de tercers.</p>

<h2>5. Ús acceptable</h2>
<p>No pots usar el servei per a activitats il·lícites, enviar spam, intentar accedir a comptes d'altres usuaris
ni sobrecarregar els nostres sistemes de forma intencionada.</p>

<h2>6. Disponibilitat</h2>
<p>No garantim una disponibilitat del 100%. Podem suspendre el servei temporalment per manteniment o per raons
tècniques o de seguretat.</p>

<h2>7. Modificacions</h2>
<p>Podem modificar aquests termes amb 30 dies d'antelació. L'ús continuat del servei implica acceptació dels canvis.</p>

<h2>8. Llei aplicable</h2>
<p>Aquests termes es regeixen per la llei espanyola. Qualsevol disputa es resoldrà als tribunals competents de Barcelona.</p>
$TERMS$ WHERE type = 'TERMS';

UPDATE legal_documents SET content = $COOKIES$
<h2>1. Què són les cookies?</h2>
<p>Les cookies són petits fitxers de text que els llocs web emmagatzemen al teu navegador per recordar preferències
i sessions.</p>

<h2>2. Cookies que usem</h2>
<table class="w-full text-sm border-collapse mt-2 mb-4">
  <thead>
    <tr class="bg-gray-100">
      <th class="text-left p-2 border border-gray-200">Nom</th>
      <th class="text-left p-2 border border-gray-200">Tipus</th>
      <th class="text-left p-2 border border-gray-200">Durada</th>
      <th class="text-left p-2 border border-gray-200">Finalitat</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td class="p-2 border border-gray-200"><code>accessToken</code></td>
      <td class="p-2 border border-gray-200">Tècnica (necessària)</td>
      <td class="p-2 border border-gray-200">Sessió</td>
      <td class="p-2 border border-gray-200">Autenticació JWT</td>
    </tr>
    <tr>
      <td class="p-2 border border-gray-200"><code>refreshToken</code></td>
      <td class="p-2 border border-gray-200">Tècnica (necessària)</td>
      <td class="p-2 border border-gray-200">7 dies</td>
      <td class="p-2 border border-gray-200">Renovació automàtica de sessió</td>
    </tr>
    <tr>
      <td class="p-2 border border-gray-200"><code>cookieConsent</code></td>
      <td class="p-2 border border-gray-200">Preferències</td>
      <td class="p-2 border border-gray-200">1 any</td>
      <td class="p-2 border border-gray-200">Recordar la teva elecció de cookies</td>
    </tr>
  </tbody>
</table>

<h2>3. Cookies de tercers</h2>
<p>No usem cookies de publicitat ni de seguiment de tercers. El mapa de l'aplicació usa tiles d'OpenStreetMap,
que pot establir cookies pròpies (vegeu la seva <a href="https://wiki.osmfoundation.org/wiki/Privacy_Policy" target="_blank" rel="noopener">política de privacitat</a>).</p>

<h2>4. Gestió de cookies</h2>
<p>Pots desactivar les cookies al teu navegador, però el servei pot deixar de funcionar correctament.
Les cookies tècniques són imprescindibles per a l'autenticació.</p>

<h2>5. Consentiment</h2>
<p>Quan uses TravelAI per primera vegada, et demanem el consentiment per a les cookies no essencials.
Pots canviar la teva elecció en qualsevol moment des d'<a href="/my-data">Les meves dades</a>.</p>
$COOKIES$ WHERE type = 'COOKIES';

INSERT INTO legal_documents (type, version, content, active, published_at)
VALUES ('LEGAL_NOTICE', '1.0', $LEGAL$
<h2>1. Titular</h2>
<p>TravelAI és un projecte acadèmic de demostració.
Contacte: <a href="mailto:privacidad@travelai.local">privacidad@travelai.local</a></p>

<h2>2. Propietat intel·lectual</h2>
<p>El codi font, el disseny i el contingut propi de TravelAI estan protegits pels drets d'autor dels seus autors.
El contingut generat pels usuaris pertany als seus respectius autors.</p>

<h2>3. Exempció de responsabilitat</h2>
<p>TravelAI no garanteix l'exactitud dels itineraris generats per IA. Aquests s'ofereixen com a suggeriments
i no substitueixen l'assessorament professional de viatges. L'usuari és responsable de verificar la informació
abans de fer-la servir per planificar un viatge real.</p>

<h2>4. Limitació de responsabilitat</h2>
<p>No som responsables dels danys derivats de l'ús de la plataforma, de la inexactitud del contingut generat per IA,
ni dels serveis de tercers (mapes, transport, allotjament) mencionats als itineraris.</p>

<h2>5. Llei aplicable</h2>
<p>Aquesta plataforma es regeix per la legislació espanyola i la normativa europea aplicable (RGPD, ePrivacy).</p>
$LEGAL$, true, NOW())
ON CONFLICT (type, version) DO UPDATE SET content = EXCLUDED.content, active = true;
