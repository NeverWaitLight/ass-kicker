/**
 * Benchmark 渠道配置种子数据（8 条：四大 ChannelType × 各子协议）
 *
 * Robo 3T：连接目标库后，在 Shell 编辑器中打开本文件并执行（Execute Script / 执行脚本）。
 * 无需先切换库：脚本内已 getSiblingDB("asskicker")。
 *
 * 命令行 mongo 旧 shell / mongosh 亦可执行本文件。
 *
 * 可重复执行：deleteMany 固定 _id 字符串 "1001"–"1008" 后 insertMany
 */

var databaseName = "asskicker";
var dbBench = db.getSiblingDB(databaseName);
var coll = dbBench.t_channel_config;

var CHANNEL_IDS = ["1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008"];

var now = new Date().getTime();

var props = {
  emailSmtp: {
    type: "SMTP",
    smtp: {
      host: "smtp.example.com",
      port: 465,
      username: "bench@example.com",
      password: "bench-smtp-password",
      sslEnabled: true,
      maxRetries: 3,
      connectionTimeout: 5000,
      readTimeout: 10000,
      retryDelay: 1000,
    },
  },
  emailHttp: {
    type: "HTTP",
    http: {
      baseUrl: "https://httpbin.org",
      path: "/post",
      apiKeyHeader: "Authorization",
      apiKey: "bench-http-api-key",
      maxRetries: 3,
      timeout: 5000,
      retryDelay: 1000,
    },
  },
  imDingTalk: {
    type: "DINGTALK",
    dingTalk: {
      webhookUrl:
        "https://oapi.dingtalk.com/robot/send?access_token=bench_dummy_access_token",
      maxRetries: 3,
      timeout: 5000,
      retryDelay: 1000,
    },
  },
  imWeCom: {
    type: "WECOM",
    wecom: {
      webhookUrl:
        "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=bench_dummy_wecom_key",
      maxRetries: 3,
      timeout: 5000,
      retryDelay: 1000,
    },
  },
  pushApns: {
    type: "APNS",
    apns: {
      teamId: "BENCH1TEAM",
      keyId: "BENCHKEY01",
      bundleId: "com.example.asskicker.bench",
      p8KeyContent:
        "-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgBenchPlaceholderKey\n-----END PRIVATE KEY-----\n",
      production: false,
      maxRetries: 3,
      timeout: 10000,
      retryDelay: 1000,
    },
  },
  pushFcm: {
    type: "FCM",
    fcm: {
      serviceAccountJson: JSON.stringify({
        type: "service_account",
        project_id: "bench-project",
        private_key_id: "bench_key_id",
        private_key:
          "-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgBench\n-----END PRIVATE KEY-----\n",
        client_email: "bench@bench-project.iam.gserviceaccount.com",
        client_id: "123456789",
      }),
      maxRetries: 3,
      timeout: 10000,
      retryDelay: 1000,
    },
  },
  smsAliyun: {
    type: "ALIYUN",
    aliyun: {
      accessKeyId: "bench-aliyun-ak",
      accessKeySecret: "bench-aliyun-secret",
      signName: "BenchSign",
      templateCode: "SMS_BENCH_CODE",
      templateParamKey: "content",
      regionId: "cn-hangzhou",
      maxRetries: 3,
      timeout: 10000,
      retryDelay: 1000,
    },
  },
  smsTencent: {
    type: "TENCENT",
    tencent: {
      secretId: "bench-tencent-secret-id",
      secretKey: "bench-tencent-secret-key",
      sdkAppId: "1400000000",
      signName: "BenchSign",
      templateId: "2000000",
      region: "ap-guangzhou",
      maxRetries: 3,
      timeout: 10000,
      retryDelay: 1000,
    },
  },
};

coll.deleteMany({ _id: { $in: CHANNEL_IDS } });

coll.insertMany(
  [
    {
      _id: CHANNEL_IDS[0],
      name: "Benchmark EMAIL / SMTP",
      type: "EMAIL",
      description: "Benchmark seed: EmailChannelType.SMTP",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.emailSmtp),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[1],
      name: "Benchmark EMAIL / HTTP",
      type: "EMAIL",
      description: "Benchmark seed: EmailChannelType.HTTP",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.emailHttp),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[2],
      name: "Benchmark IM / DINGTALK",
      type: "IM",
      description: "Benchmark seed: IMChannelType.DINGTALK",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.imDingTalk),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[3],
      name: "Benchmark IM / WECOM",
      type: "IM",
      description: "Benchmark seed: IMChannelType.WECOM",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.imWeCom),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[4],
      name: "Benchmark PUSH / APNS",
      type: "PUSH",
      description: "Benchmark seed: PushChannelType.APNS",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.pushApns),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[5],
      name: "Benchmark PUSH / FCM",
      type: "PUSH",
      description: "Benchmark seed: PushChannelType.FCM",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.pushFcm),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[6],
      name: "Benchmark SMS / ALIYUN",
      type: "SMS",
      description: "Benchmark seed: SmsChannelType.ALIYUN",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.smsAliyun),
      created_at: now,
      updated_at: now,
    },
    {
      _id: CHANNEL_IDS[7],
      name: "Benchmark SMS / TENCENT",
      type: "SMS",
      description: "Benchmark seed: SmsChannelType.TENCENT",
      include_recipient_regex: null,
      exclude_recipient_regex: null,
      properties_json: JSON.stringify(props.smsTencent),
      created_at: now,
      updated_at: now,
    },
  ],
  { ordered: true },
);

print(
  "Inserted " +
  coll.find({ _id: { $in: CHANNEL_IDS } }).count() +
  " benchmark channel configs into " +
  databaseName +
  ".t_channel_config",
);
