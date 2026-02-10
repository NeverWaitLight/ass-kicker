## ADDED Requirements

### Requirement: User Menu Display
系统应提供一个用户下拉菜单，当用户点击用户名时显示。

#### Scenario: 用户点击用户名
- **WHEN** 用户点击右上角的用户名
- **THEN** 系统应显示包含用户设置和退出选项的下拉菜单

### Requirement: User Menu Options
用户下拉菜单应包含用户设置和退出选项。

#### Scenario: 用户查看菜单选项
- **WHEN** 用户打开用户下拉菜单
- **THEN** 系统应显示用户设置和退出选项

### Requirement: Logout Functionality
用户应能够通过用户下拉菜单中的退出选项退出系统。

#### Scenario: 用户选择退出
- **WHEN** 用户点击用户下拉菜单中的退出选项
- **THEN** 系统应执行登出操作并重定向到登录页面

### Requirement: Settings Access
用户应能够通过用户下拉菜单中的设置选项访问用户设置。

#### Scenario: 用户选择设置
- **WHEN** 用户点击用户下拉菜单中的设置选项
- **THEN** 系统应导航到用户设置页面