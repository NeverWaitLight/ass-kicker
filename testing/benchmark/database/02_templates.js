/**
 * Benchmark 模板与多语言内容种子数据
 * - t_template：每个 ChannelType 一条（共 4 条）
 * - t_language_template：每个模板 × Language 枚举（共 20 条）
 *
 * Robo 3T：连接后在 Shell 中打开本文件并执行；须在 01_channel_config.js 之后或独立执行均可。
 *
 * 可重复执行：先删语言行 _id 3001–3020，再删模板 2001–2004
 */

var databaseName = "asskicker";
var dbBench = db.getSiblingDB(databaseName);
var tplColl = dbBench.t_template;
var ltColl = dbBench.t_language_template;

var TEMPLATE_IDS = ["2001", "2002", "2003", "2004"];

var LANGUAGE_TEMPLATE_IDS = [];
var i;
for (i = 3001; i <= 3020; i++) {
  LANGUAGE_TEMPLATE_IDS.push(String(i));
}

var now = new Date().getTime();

var languages = ["ZH_CN", "ZH_TW", "EN", "FR", "DE"];

var templates = [
  {
    _id: TEMPLATE_IDS[0],
    name: "Benchmark SMS Template",
    code: "bench-sms",
    description: "Benchmark seed template for ChannelType.SMS",
    channel_type: "SMS",
    attributes: { scenario: "benchmark" },
    created_at: now,
    updated_at: now,
  },
  {
    _id: TEMPLATE_IDS[1],
    name: "Benchmark EMAIL Template",
    code: "bench-email",
    description: "Benchmark seed template for ChannelType.EMAIL",
    channel_type: "EMAIL",
    attributes: { scenario: "benchmark" },
    created_at: now,
    updated_at: now,
  },
  {
    _id: TEMPLATE_IDS[2],
    name: "Benchmark IM Template",
    code: "bench-im",
    description: "Benchmark seed template for ChannelType.IM",
    channel_type: "IM",
    attributes: { scenario: "benchmark" },
    created_at: now,
    updated_at: now,
  },
  {
    _id: TEMPLATE_IDS[3],
    name: "Benchmark PUSH Template",
    code: "bench-push",
    description: "Benchmark seed template for ChannelType.PUSH",
    channel_type: "PUSH",
    attributes: { scenario: "benchmark" },
    created_at: now,
    updated_at: now,
  },
];

function contentFor(channelKey, lang) {
  var base = {
    sms: {
      ZH_CN: "【Bench】您的验证码是 {{code}}，5 分钟内有效。",
      ZH_TW: "【Bench】您的驗證碼是 {{code}}，5 分鐘內有效。",
      EN: "[Bench] Your verification code is {{code}}, valid for 5 minutes.",
      FR: "[Bench] Votre code de vérification est {{code}}, valide 5 minutes.",
      DE: "[Bench] Ihr Bestätigungscode lautet {{code}}, gültig für 5 Minuten.",
    },
    email: {
      ZH_CN: "Bench 邮件：验证码 {{code}}，请勿泄露。",
      ZH_TW: "Bench 郵件：驗證碼 {{code}}，請勿洩露。",
      EN: "Bench email: your code is {{code}}. Do not share it.",
      FR: "Bench e-mail : votre code est {{code}}. Ne le partagez pas.",
      DE: "Bench E-Mail: Ihr Code ist {{code}}. Geben Sie ihn nicht weiter.",
    },
    im: {
      ZH_CN: "Bench IM 通知：验证码 {{code}}",
      ZH_TW: "Bench IM 通知：驗證碼 {{code}}",
      EN: "Bench IM: verification code {{code}}",
      FR: "Bench IM : code {{code}}",
      DE: "Bench IM: Code {{code}}",
    },
    push: {
      ZH_CN: "Bench 推送：验证码 {{code}}",
      ZH_TW: "Bench 推送：驗證碼 {{code}}",
      EN: "Bench push: code {{code}}",
      FR: "Bench push : code {{code}}",
      DE: "Bench Push: Code {{code}}",
    },
  };
  return base[channelKey][lang];
}

var templateMeta = [
  { templateIdStr: "2001", key: "sms" },
  { templateIdStr: "2002", key: "email" },
  { templateIdStr: "2003", key: "im" },
  { templateIdStr: "2004", key: "push" },
];

ltColl.deleteMany({ _id: { $in: LANGUAGE_TEMPLATE_IDS } });
tplColl.deleteMany({ _id: { $in: TEMPLATE_IDS } });

tplColl.insertMany(templates, { ordered: true });

var languageRows = [];
var ltIndex = 0;
var mi;
var lj;
var meta;
var lang;
for (mi = 0; mi < templateMeta.length; mi++) {
  meta = templateMeta[mi];
  for (lj = 0; lj < languages.length; lj++) {
    lang = languages[lj];
    languageRows.push({
      _id: LANGUAGE_TEMPLATE_IDS[ltIndex],
      template_id: meta.templateIdStr,
      language: lang,
      content: contentFor(meta.key, lang),
      created_at: now,
      updated_at: now,
    });
    ltIndex += 1;
  }
}

ltColl.insertMany(languageRows, { ordered: true });

print(
  "Inserted " +
    tplColl.find({ _id: { $in: TEMPLATE_IDS } }).count() +
    " templates and " +
    ltColl.find({ _id: { $in: LANGUAGE_TEMPLATE_IDS } }).count() +
    " language templates into " +
    databaseName,
);
