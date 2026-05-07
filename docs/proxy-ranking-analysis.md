# Proxy Ranking Analysis — RU/Windows Segment

## Контекст и мотивация

Сервис выдаёт пользователям MTProto-прокси для доступа к Telegram. Текущий алгоритм
выбирает «лучший» прокси из общего пула на основе score, latency и статуса верификации
без учёта сегмента пользователя и истории его попыток в текущей сессии.

**Наблюдаемая проблема:** thrash-паттерн — пользователь возвращается за новым прокси
по 5–10+ раз за сессию, потому что один и тот же неработающий прокси стоит первым в
очереди для его сегмента.

Аналитика в PostHog за 7 дней (сегмент RU/Windows, ~1 000 сессий) позволила
идентифицировать конкретные прокси-аутсайдеры и предложить дешёвое исправление
первого этапа: per-session blacklist + жёсткое понижение аутсайдеров.

---

## Проблема метрики `worked_clicked`

Пользователь явно нажимает кнопку «Сработало» в UI после того, как прокси подключился.
На первый взгляд — идеальный сигнал.

**Механизм занижения:** если прокси сработал, пользователь уходит в Telegram и больше
не возвращается на страницу. Кнопка «Сработало» остаётся ненажатой. Конверсия в
явный фидбек — единицы процентов от реальных успехов.

Следствие: `worked_clicked` отражает лишь поведение пользователей, которые вернулись
на сайт после успешного подключения (редкий, нерепрезентативный сегмент). Ранжирование
по этой метрике давало ложные сигналы.

---

## Метрика `likely_worked` (primary)

**Логика:** прокси считается «скорее всего сработавшим», если выполнены оба условия:
1. Он был **последним** `best_proxy_shown` в сессии пользователя.
2. В этой же сессии пользователь дошёл до `open_telegram_link`.

Интерпретация: пользователь кликнул «Открыть в Telegram» на конкретном прокси и
больше не запрашивал следующий. Если бы не сработало — он вернулся бы за другим.

**Три случая шума (caveats):**
1. **Реальный успех** — прокси подключился, пользователь ушёл в TG. Это нужный сигнал.
2. **Бросил продукт** — пользователь закрыл вкладку, не попробовав. `likely_worked`
   засчитает последний показанный прокси как успех ложно. Частота зависит от bounce rate.
3. **PostHog session timeout** — если между событиями прошло >30 минут, PostHog
   открывает новую сессию. Событие `open_telegram_link` может оказаться в другой сессии,
   не там, где `best_proxy_shown`. Прокси не получит зачёт, хотя сработал.

Несмотря на шум, метрика на достаточной выборке (≥10 показов) значительно точнее
`worked_clicked` и позволяет уверенно выявлять аутсайдеров с 0% при 10–80+ показах.

---

## Результаты по сегменту RU/Windows (7 дней)

### Топ — рабочие прокси

| proxy_id | shown | likely_worked | success_pct |
|----------|------:|------------:|------------:|
| 643      | 14    | 13          | 92.9%       |
| 345      | 7     | 6           | 85.7%       |
| 1027     | 12    | 8           | 66.7%       |
| 965      | 82    | 38          | 46.3%       |
| 877      | 9     | 4           | 44.4%       |

`965` — наиболее достоверная оценка: большая выборка, результат устойчив.

### Аутсайдеры — нерабочие прокси

| proxy_id | shown | likely_worked | success_pct |
|----------|------:|------------:|------------:|
| 678      | 16    | 0           | 0%          |
| 776      | 10    | 0           | 0%          |
| 883      | 9     | 0           | 0%          |
| 1033     | 5     | 0           | 0%          |
| 688      | 76    | 12          | 15.8%       |
| 879      | 15    | 2           | 13.3%       |

`688` и `879` — большая выборка, результат статистически значим.

### Текущий дефолтный прокси

| proxy_id | shown | success_pct |
|----------|------:|------------:|
| 612      | 183   | 30.1%       |

Средний результат. Не лучший кандидат для дефолта в этом сегменте.

---

## Открытые вопросы и направления развития

- **Другие сегменты:** матрица proxy × segment не построена. DE/Android, US/iOS и т.д.
  могут давать принципиально другие рейтинги. Глобальный blacklist строить нельзя.
- **Decay:** прокси умирают и возрождаются. Overrides нужно пересматривать ≥раз в неделю.
- **Composite score:** сигналы `next_proxy_clicked`, `time_to_return`, `worked_clicked`
  можно взвесить вместе — см. Этап 3 roadmap.
- **Доверительный интервал:** при малых выборках (< 10) результат ненадёжен.
  Нужен Wilson score или Bayesian smoothing прежде чем автоматизировать.

---

## Приложение: SQL-запросы (PostHog HogQL)

### Метрика 1: worked_clicked rate (не primary, занижена)

```sql
WITH shows AS (
  SELECT properties.proxy_id AS proxy_id, person_id
  FROM events
  WHERE event = 'best_proxy_shown'
    AND timestamp > now() - INTERVAL 7 DAY
    AND properties.$geoip_country_code = 'RU'
    AND properties.$os = 'Windows'
),
worked AS (
  SELECT DISTINCT person_id, properties.proxy_id AS proxy_id
  FROM events
  WHERE event = 'worked_clicked'
    AND timestamp > now() - INTERVAL 7 DAY
)
SELECT
  s.proxy_id,
  count(DISTINCT s.person_id) AS users_shown,
  count(DISTINCT w.person_id) AS users_worked,
  round(count(DISTINCT w.person_id) * 100.0
        / count(DISTINCT s.person_id), 1) AS success_pct
FROM shows s
LEFT JOIN worked w ON s.person_id = w.person_id AND s.proxy_id = w.proxy_id
GROUP BY s.proxy_id
HAVING users_shown >= 10
ORDER BY success_pct ASC;
```

### Метрика 2: likely_worked (PRIMARY)

```sql
WITH telegram_sessions AS (
  SELECT DISTINCT person_id, properties.$session_id AS session_id
  FROM events
  WHERE event = 'open_telegram_link'
    AND timestamp > now() - INTERVAL 7 DAY
),
proxy_shows AS (
  SELECT
    e.person_id,
    e.properties.$session_id AS session_id,
    e.properties.proxy_id AS proxy_id,
    e.timestamp
  FROM events e
  JOIN telegram_sessions ts
    ON e.person_id = ts.person_id
    AND e.properties.$session_id = ts.session_id
  WHERE e.event = 'best_proxy_shown'
    AND e.timestamp > now() - INTERVAL 7 DAY
    AND e.properties.$geoip_country_code = 'RU'
    AND e.properties.$os = 'Windows'
),
session_last AS (
  SELECT person_id, session_id, max(timestamp) AS last_show_ts
  FROM proxy_shows
  GROUP BY person_id, session_id
)
SELECT
  ps.proxy_id,
  count() AS shown_count,
  countIf(ps.timestamp = sl.last_show_ts) AS likely_worked,
  round(countIf(ps.timestamp = sl.last_show_ts) * 100.0 / count(), 1) AS success_pct
FROM proxy_shows ps
JOIN session_last sl ON ps.person_id = sl.person_id AND ps.session_id = sl.session_id
GROUP BY ps.proxy_id
HAVING shown_count >= 5
ORDER BY success_pct DESC;
```
