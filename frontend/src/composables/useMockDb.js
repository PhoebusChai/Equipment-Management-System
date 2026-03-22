import { onBeforeUnmount, onMounted, ref } from "vue";
import { getDb, subscribeDb } from "../mock/db";

export function useMockDb() {
  const tick = ref(0);
  const db = ref(getDb());

  let unsubscribe = null;

  function refresh() {
    tick.value += 1;
    db.value = getDb();
  }

  onMounted(() => {
    unsubscribe = subscribeDb(refresh);
    refresh();
  });

  onBeforeUnmount(() => {
    if (unsubscribe) unsubscribe();
  });

  return {
    tick,
    db,
    refresh
  };
}

