<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMe } from '@/api/users'
import { useSessionStore } from '@/stores/session'
import { formatDate, PLAN_LABEL } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import PlanBadge from '@/components/layout/PlanBadge.vue'
import WithdrawDialog from '@/components/account/WithdrawDialog.vue'

const router = useRouter()
const session = useSessionStore()
const state = ref('loading')
const error = ref(null)
const withdrawOpen = ref(false)

async function load() {
  state.value = 'loading'
  error.value = null
  try {
    session.setUser(await fetchMe())
    state.value = 'ready'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

async function onWithdrawn() {
  session.clear()
  await router.replace({ name: 'login' })
}

onMounted(load)
</script>

<template>
  <PageHeader title="내 정보" description="계정과 이용 등급을 확인할 수 있습니다." />
  <StateBlock :state="state" :message="error?.message" @retry="load">
    <section v-if="session.user" class="profile-card">
      <div class="avatar" aria-hidden="true">{{ session.user.name?.slice(0, 1) }}</div>
      <div class="profile-card__heading">
        <h2>{{ session.user.name }}</h2>
        <PlanBadge :plan="session.user.plan" />
      </div>
      <dl>
        <div><dt>이메일</dt><dd>{{ session.user.email }}</dd></div>
        <div><dt>이용 등급</dt><dd>{{ PLAN_LABEL[session.user.plan] ?? session.user.plan }}</dd></div>
        <div><dt>가입일</dt><dd>{{ formatDate(session.user.createdAt) }}</dd></div>
      </dl>
      <router-link v-if="!session.isPro" :to="{ name: 'upgrade' }" class="upgrade-link">PRO 기능 알아보기</router-link>
    </section>

    <section class="danger-zone">
      <div>
        <h2>회원 탈퇴</h2>
        <p>탈퇴하면 계정과 모든 기록을 복구할 수 없습니다.</p>
      </div>
      <button type="button" @click="withdrawOpen = true">탈퇴하기</button>
    </section>
  </StateBlock>
  <WithdrawDialog :open="withdrawOpen" @close="withdrawOpen = false" @withdrawn="onWithdrawn" />
</template>

<style scoped>
.profile-card, .danger-zone { padding: var(--space-6); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.profile-card { display: grid; grid-template-columns: auto 1fr; gap: var(--space-5); }
.avatar { display: grid; width: 64px; height: 64px; place-items: center; color: white; font-size: 1.5rem; font-weight: 900; background: var(--color-primary); border-radius: 50%; }
.profile-card__heading { display: flex; align-items: center; gap: var(--space-3); }
.profile-card__heading h2 { margin: 0; }
dl { grid-column: 1 / -1; margin: 0; border-top: 1px solid var(--color-border); }
dl div { display: grid; grid-template-columns: 120px 1fr; gap: var(--space-4); padding: var(--space-3) 0; border-bottom: 1px solid var(--color-border); }
dt { color: var(--color-text-muted); }
dd { margin: 0; font-weight: 700; }
.upgrade-link { grid-column: 1 / -1; justify-self: start; }
.danger-zone { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin-top: var(--space-8); border-color: #efcaca; }
.danger-zone h2, .danger-zone p { margin: 0; }
.danger-zone p { color: var(--color-text-muted); }
.danger-zone button { padding: 9px 13px; color: var(--color-danger); background: var(--color-danger-weak); border: 1px solid #efcaca; border-radius: var(--radius-1); }
@media (max-width: 560px) { .danger-zone { align-items: stretch; flex-direction: column; } }
</style>
