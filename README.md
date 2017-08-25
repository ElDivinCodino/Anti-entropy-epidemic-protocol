##DS2-Project

####Efficient Reconciliation and Flow Control for Anti-Entropy Protocols

Gossip protocols, useful for replicating state without string consistency requirements. 

Issue: How Gossip behaves under *high update load*. Gossip capacity is limited by network bandwidth and CPU cycles for generating and processing messages.

Have to:

- fix gossip rate
- fix maximum message size

--

Each process has a *local* view of each other.

Each participant state is replicated to all other participants > ??
$\mu_p(q)$ indica che il processo p conosce lo stato di q.

Reconciliation: take the value with the highest version number.

Each participant has a set of *Peers* that is gossips with. Periodically a process gossips with its peers choosing one at random.

**push-gossip**: a participant $p$ sends its whole state $\mu_p$ to $q$. So it sends all the states of the processes it knows of.

**pull-gossip**: $p$ sends a *digest* of $\mu_p$ (with the values removed) and $q$ sends back just the updates.

**push-pull**: merge of the two. This is the most efficient style.

**delta**: a tuple (p, k, v, n)

###Reconciliation (improved)

#### Precise reconciliation

Send only necessary updates.

Send exactly those mappings that are more recent than those of the peer.

####Scuttlebutt reconciliation

Ogni qual volta un processo vuole aggiornare un elemento locale, deve utilizzare un numero di version più alto del massimo attuale.

Prima di tutto p e q si scambiano i loro massimi dei processi di cui hanno delle tuple.

Poi p invia a q tutte le tuple che hanno un numero di versione maggiore rispetto al massimo che vede q per quel processo.
E viceversa per q.

se c'è una mtu (quindi un numero massimo di delta da mandare) vengono mandati prima i delta che hanno un version number minore. (Con un ordinamento singolo per ciascun processo)

Possible types of orderings:

- scuttle-breath: Prima si esegue un ordinamento locale per ogni processo, poi si prende in ordine il primo elemento da ogni processo, poi il secondo elemento da ogni processo e cosi via. L'ordine con cui si prendono questi elementi (quindi l'ordine dei processi) è casuale e cambia ad ogni messaggio (per evitare bias).
- scuttle-depth: L'ordinamento va in base al numero di delta che un processo può inviare. Quindi il processo che ha più delta invia per primo. Nel caso ci sia lo stesso numero di delta per diversi processi si sceglie a random.
