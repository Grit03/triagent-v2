-- 같은 알림(fingerprint)으로 열린 장애가 두 건 이상 생기지 않도록 DB가 막습니다.
-- 열려 있는 동안에만 fingerprint를 넣고, 해소되면 null로 바꿉니다. unique 제약은 null끼리는 비교하지 않으므로
-- 해소된 장애는 몇 건이든 쌓일 수 있습니다. 부분 인덱스(where status = 'OPEN')는 H2가 지원하지 않아 이 방식을 씁니다.
alter table incident add column open_fingerprint varchar(64);

-- 이미 중복으로 열린 장애가 있으면 가장 먼저 열린 한 건에만 값을 넣습니다. 나머지는 다음 resolved 알림 때 함께 닫힙니다.
update incident
set open_fingerprint = fingerprint
where status = 'OPEN'
  and id = (select min(other.id) from incident other where other.fingerprint = incident.fingerprint and other.status = 'OPEN');

alter table incident add constraint uk_incident_open_fingerprint unique (open_fingerprint);
