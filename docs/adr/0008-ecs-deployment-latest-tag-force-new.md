# ADR-0008: ECS 배포는 `:latest` 태그 + `force-new-deployment`

- 상태: Accepted
- 날짜: 2026-05-26
- 관련: M9 Deployment, GitHub Actions, ECS service

## Context
프로덕션 hygiene 관점의 정석은:
- 이미지 태그를 immutable한 값(git SHA, 빌드 번호)으로 고정.
- 새 task definition revision을 만들고 service 업데이트.

이 방식의 장점은 "현재 떠 있는 컨테이너가 어떤 commit인지" 추적이 정확하고, 롤백이 결정적이다.

그러나 AdoptLoop는 7일 토이 프로젝트다. 위 방식은 GitHub Actions 워크플로우 복잡도, task def 관리, IAM 정책을 모두 늘린다. 일정 안에 끝낼 가치를 의도적으로 평가했다.

## Decision
- 이미지는 `:latest` 태그로 ECR에 push (다른 태그 없음).
- 배포는 `aws ecs update-service --force-new-deployment`로 롤아웃.
- 롤백은 "이전 commit을 빌드해서 다시 `:latest`로 push 후 force-new-deployment".

## Consequences
- ✅ 배포 워크플로우가 한 화면에 들어가는 길이로 유지.
- ✅ task def 관리·revision 추적 없음.
- ❌ "지금 떠 있는 게 어느 commit인지"가 ECR 디지스트 + push 시각으로만 추적됨 → 정확한 시점-이미지 대응 어려움.
- ❌ 롤백이 비결정적 (다시 빌드해야 함, 빌드 환경이 바뀌면 결과도 다를 수 있음).
- **이전 가능 조건**: 사용자 1명을 넘어 프로덕션화하면 immutable tag로 즉시 supersedes.
