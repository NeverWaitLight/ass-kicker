import { test, expect } from '@playwright/test'

test('channel management flow', async ({ page }) => {
  const permissions = ['channel:read', 'channel:create', 'channel:update', 'channel:delete']
  await page.addInitScript((perms) => {
    localStorage.setItem('access_token', 'e2e-token')
    localStorage.setItem('auth_user', JSON.stringify({
      username: 'e2e',
      role: 'USER',
      permissions: perms
    }))
  }, permissions)

  let channels = [
    {
      id: '1',
      name: 'Email Channel',
      type: 'EMAIL',
      description: '默认渠道',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      properties: {}
    }
  ]

  await page.route('**/api/channels', async (route) => {
    const request = route.request()
    if (request.method() === 'GET') {
      return route.fulfill({ status: 200, json: channels })
    }
    if (request.method() === 'POST') {
      const payload = request.postDataJSON()
      const created = {
        id: '2',
        ...payload,
        createdAt: Date.now(),
        updatedAt: Date.now()
      }
      channels = [created, ...channels]
      return route.fulfill({ status: 201, json: created })
    }
    return route.fallback()
  })

  await page.route('**/api/channels/types', async (route) => {
    return route.fulfill({ status: 200, json: ['SMS', 'EMAIL', 'IM', 'PUSH'] })
  })

  await page.route('**/api/channels/email-protocols', async (route) => {
    return route.fulfill({
      status: 200,
      json: {
        defaultProtocol: 'SMTP',
        protocols: [
          {
            protocol: 'SMTP',
            label: 'SMTP',
            propertyKey: 'smtp',
            fields: [
              { key: 'host', label: 'SMTP 主机', required: true, defaultValue: '' },
              { key: 'port', label: '端口', required: true, defaultValue: '465' },
              { key: 'username', label: '用户名', required: true, defaultValue: '' },
              { key: 'password', label: '密码', required: true, defaultValue: '' }
            ]
          },
          {
            protocol: 'HTTP_API',
            label: 'HTTP API',
            propertyKey: 'httpApi',
            fields: [
              { key: 'baseUrl', label: 'API 基础地址', required: true, defaultValue: '' }
            ]
          }
        ]
      }
    })
  })

  await page.route('**/api/channels/test-send', async (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 200, json: { success: true, messageId: 'test-1' } })
    }
    return route.fallback()
  })

  await page.route('**/api/channels/*', async (route) => {
    const request = route.request()
    const id = request.url().split('/').pop()
    if (request.method() === 'GET') {
      const found = channels.find((ch) => ch.id === id)
      if (!found) {
        return route.fulfill({ status: 404, body: 'not found' })
      }
      return route.fulfill({ status: 200, json: found })
    }
    if (request.method() === 'PUT') {
      const payload = request.postDataJSON()
      channels = channels.map((ch) =>
        ch.id === id
          ? { ...ch, ...payload, updatedAt: Date.now() }
          : ch
      )
      const updated = channels.find((ch) => ch.id === id)
      return route.fulfill({ status: 200, json: updated })
    }
    if (request.method() === 'DELETE') {
      channels = channels.filter((ch) => ch.id !== id)
      return route.fulfill({ status: 204, body: '' })
    }
    return route.fallback()
  })

  await page.goto('/channels')
  await expect(page.getByText('通道')).toBeVisible()
  await expect(page.getByText('Email Channel')).toBeVisible()

  await page.getByRole('button', { name: '测试' }).first().click()
  const listTestDialog = page.getByRole('dialog')
  await listTestDialog.getByPlaceholder('请输入目标地址').fill('test@example.com')
  await listTestDialog.getByPlaceholder('请输入测试内容').fill('hello')
  await listTestDialog.getByRole('button', { name: '发送' }).click()
  await expect(listTestDialog.getByText('测试发送成功')).toBeVisible()
  await listTestDialog.getByRole('button', { name: '取消' }).click()

  await page.getByRole('button', { name: '新建通道' }).click()
  await page.getByPlaceholder('请输入通道名称').fill('Email Channel 2')
  await page.getByRole('combobox').first().click()
  await page.getByRole('option', { name: 'EMAIL' }).click()
  await expect(page.getByDisplayValue('host')).toBeVisible()
  await expect(page.getByDisplayValue('port')).toBeVisible()
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('Email Channel 2')).toBeVisible()

  await page.getByRole('button', { name: '编辑' }).first().click()
  await page.getByPlaceholder('请输入通道名称').fill('Push Channel')
  await page.getByRole('combobox').first().click()
  await page.getByRole('option', { name: 'PUSH' }).click()
  await page.getByRole('button', { name: '测试' }).click()
  const testDialog = page.getByRole('dialog')
  await testDialog.getByPlaceholder('请输入目标地址').fill('test@example.com')
  await testDialog.getByPlaceholder('请输入测试内容').fill('hello')
  await testDialog.getByRole('button', { name: '发送' }).click()
  await expect(testDialog.getByText('测试发送成功')).toBeVisible()
  await testDialog.getByRole('button', { name: '取消' }).click()
  await page.getByRole('combobox').nth(1).click()
  await page.getByRole('option', { name: '对象' }).click()
  await page.getByPlaceholder('对象键').fill('region')
  await page.getByPlaceholder('对象值').fill('cn')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('Push Channel')).toBeVisible()

  await page.getByRole('button', { name: '删除' }).first().click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: '删除' }).click()
  await expect(page.getByText('Email Channel 2')).toHaveCount(0)
})
