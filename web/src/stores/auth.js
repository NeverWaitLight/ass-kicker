import { ref } from 'vue'
import { getAccessToken, getUser } from '../utils/auth'

export const isAuthenticated = ref(!!getAccessToken())
export const currentUser = ref(getUser())

export const syncAuth = () => {
  isAuthenticated.value = !!getAccessToken()
  currentUser.value = getUser()
}
