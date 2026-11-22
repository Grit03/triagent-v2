-- 동시 승인, 거절을 막는 낙관적 락용 버전 컬럼. 기존 행은 0부터 시작합니다.
alter table remediation_action add column version bigint default 0 not null;
