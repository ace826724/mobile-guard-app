"use strict";

/*
 * MGA - Mobile Guard App
 * Main JavaScript Controller
 * Version: 1.1.0
 *
 * Ana ekran, engelleme kayıtları ve güvenilir siteler yönetimi.
 * VPN motoru henüz entegre edilmemiştir.
 */

const MGA = {
  version: "1.1.0",

  storageKey: "mga_allowlist_v1",

  state: {
    protectionActive: false,
    blockedCount: 0,
    checkedCount: 0,
    logs: [],
    allowlist: []
  },

  elements: {},

  init() {
    this.cacheElements();
    this.loadAllowlist();
    this.bindEvents();
    this.updateInterface();
    this.showHomeScreen();

    console.log("MGA Mobile Guard App initialized.");
  },

  cacheElements() {
    this.elements = {
      homeScreen: document.getElementById("homeScreen"),
      logsScreen: document.getElementById("logsScreen"),
      allowlistScreen: document.getElementById("allowlistScreen"),

      protectionButton: document.getElementById("protectionButton"),
      protectionStatus: document.getElementById("protectionStatus"),
      protectionDescription: document.getElementById("protectionDescription"),
      shieldIcon: document.getElementById("shieldIcon"),

      blockedCount: document.getElementById("blockedCount"),
      checkedCount: document.getElementById("checkedCount"),

      logsButton: document.getElementById("logsButton"),
      backToHomeButton: document.getElementById("backToHomeButton"),

      logsBlockedCount: document.getElementById("logsBlockedCount"),
      logsRecordCount: document.getElementById("logsRecordCount"),
      emptyLogsMessage: document.getElementById("emptyLogsMessage"),
      logsList: document.getElementById("logsList"),

      allowlistButton: document.getElementById("allowlistButton"),
      backFromAllowlistButton: document.getElementById("backFromAllowlistButton"),
      allowlistDomainInput: document.getElementById("allowlistDomainInput"),
      addAllowlistButton: document.getElementById("addAllowlistButton"),
      allowlistMessage: document.getElementById("allowlistMessage"),
      allowlistCount: document.getElementById("allowlistCount"),
      allowlistEmptyMessage: document.getElementById("allowlistEmptyMessage"),
      allowlistList: document.getElementById("allowlistList"),

      settingsButton: document.getElementById("settingsButton")
    };
  },

  bindEvents() {
    this.elements.protectionButton.addEventListener(
      "click",
      () => this.toggleProtection()
    );

    this.elements.logsButton.addEventListener(
      "click",
      () => this.showLogsScreen()
    );

    this.elements.backToHomeButton.addEventListener(
      "click",
      () => this.showHomeScreen()
    );

    this.elements.allowlistButton.addEventListener(
      "click",
      () => this.showAllowlistScreen()
    );

    this.elements.backFromAllowlistButton.addEventListener(
      "click",
      () => this.showHomeScreen()
    );

    this.elements.addAllowlistButton.addEventListener(
      "click",
      () => this.addAllowlistDomain()
    );

    this.elements.allowlistDomainInput.addEventListener(
      "keydown",
      (event) => {
        if (event.key === "Enter") {
          this.addAllowlistDomain();
        }
      }
    );

    this.elements.settingsButton.addEventListener(
      "click",
      () => this.showComingSoon("Ayarlar")
    );
  },

  showHomeScreen() {
    this.elements.logsScreen.hidden = true;
    this.elements.allowlistScreen.hidden = true;
    this.elements.homeScreen.hidden = false;

    window.scrollTo(0, 0);
  },

  showLogsScreen() {
    this.updateLogsScreen();

    this.elements.homeScreen.hidden = true;
    this.elements.allowlistScreen.hidden = true;
    this.elements.logsScreen.hidden = false;

    window.scrollTo(0, 0);
  },

  showAllowlistScreen() {
    this.updateAllowlistScreen();

    this.elements.homeScreen.hidden = true;
    this.elements.logsScreen.hidden = true;
    this.elements.allowlistScreen.hidden = false;

    this.elements.allowlistMessage.textContent = "";

    window.scrollTo(0, 0);
    this.elements.allowlistDomainInput.focus();
  },

  updateLogsScreen() {
    const totalBlocked = this.state.blockedCount;
    const totalRecords = this.state.logs.length;

    this.elements.logsBlockedCount.textContent = totalBlocked;
    this.elements.logsRecordCount.textContent = totalRecords;

    this.elements.emptyLogsMessage.hidden = totalRecords > 0;

    this.elements.logsList.replaceChildren();

    this.state.logs.forEach((log) => {
      const item = document.createElement("article");
      item.className = "stat-card";

      const domain = document.createElement("strong");
      domain.textContent = log.domain;

      const reason = document.createElement("p");
      reason.textContent = log.reason;

      const date = document.createElement("small");
      date.textContent = log.date;

      item.append(domain, reason, date);
      this.elements.logsList.appendChild(item);
    });
  },

  loadAllowlist() {
    try {
      const saved = localStorage.getItem(this.storageKey);

      if (!saved) {
        this.state.allowlist = [];
        return;
      }

      const parsed = JSON.parse(saved);

      if (!Array.isArray(parsed)) {
        this.state.allowlist = [];
        return;
      }

      this.state.allowlist = [
        ...new Set(
          parsed.filter(
            (domain) =>
              typeof domain === "string" &&
              this.isValidDomain(domain)
          )
        )
      ];
    } catch (error) {
      console.error("Güvenilir site listesi okunamadı.", error);
      this.state.allowlist = [];
    }
  },

  saveAllowlist() {
    try {
      localStorage.setItem(
        this.storageKey,
        JSON.stringify(this.state.allowlist)
      );

      return true;
    } catch (error) {
      console.error("Güvenilir site listesi kaydedilemedi.", error);
      this.showAllowlistMessage(
        "Liste kaydedilemedi. Depolama alanını kontrol edin."
      );

      return false;
    }
  },

  normalizeDomain(value) {
    let domain = value.trim().toLowerCase();

    domain = domain.replace(/^https?:\/\//, "");
    domain = domain.replace(/^www\./, "");

    return domain;
  },

  isValidDomain(domain) {
    if (typeof domain !== "string" || domain.length > 253) {
      return false;
    }

    const labels = domain.split(".");

    if (labels.length < 2) {
      return false;
    }

    return labels.every((label) => {
      return (
        label.length >= 1 &&
        label.length <= 63 &&
        /^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/.test(label)
      );
    }) && /^[a-z]{2,63}$/.test(labels[labels.length - 1]);
  },

  addAllowlistDomain() {
    const input = this.elements.allowlistDomainInput;
    const domain = this.normalizeDomain(input.value);

    if (!domain) {
      this.showAllowlistMessage("Lütfen bir site adresi girin.");
      input.focus();
      return;
    }

    if (!this.isValidDomain(domain)) {
      this.showAllowlistMessage(
        "Geçerli bir alan adı girin. Örnek: example.com"
      );
      input.focus();
      return;
    }

    if (this.state.allowlist.includes(domain)) {
      this.showAllowlistMessage("Bu site zaten listede bulunuyor.");
      input.select();
      return;
    }

    this.state.allowlist.push(domain);

    if (!this.saveAllowlist()) {
      this.state.allowlist.pop();
      return;
    }

    input.value = "";
    this.showAllowlistMessage("Site güvenilir listesine eklendi.");
    this.updateAllowlistScreen();
    input.focus();
  },

  removeAllowlistDomain(domain) {
    const previousList = [...this.state.allowlist];

    this.state.allowlist = this.state.allowlist.filter(
      (item) => item !== domain
    );

    if (!this.saveAllowlist()) {
      this.state.allowlist = previousList;
      return;
    }

    this.showAllowlistMessage("Site listeden kaldırıldı.");
    this.updateAllowlistScreen();
  },

  updateAllowlistScreen() {
    const total = this.state.allowlist.length;

    this.elements.allowlistCount.textContent = total;
    this.elements.allowlistEmptyMessage.hidden = total > 0;

    this.elements.allowlistList.replaceChildren();

    this.state.allowlist.forEach((domain) => {
      const item = document.createElement("article");
      item.className = "stat-card";

      const row = document.createElement("div");
      row.style.display = "flex";
      row.style.justifyContent = "space-between";
      row.style.alignItems = "center";
      row.style.gap = "12px";

      const name = document.createElement("strong");
      name.textContent = domain;
      name.style.overflowWrap = "anywhere";

      const removeButton = document.createElement("button");
      removeButton.type = "button";
      removeButton.textContent = "Sil";
      removeButton.setAttribute(
        "aria-label",
        domain + " adresini güvenilir listesinden sil"
      );

      removeButton.addEventListener("click", () => {
        this.removeAllowlistDomain(domain);
      });

      row.append(name, removeButton);
      item.appendChild(row);

      this.elements.allowlistList.appendChild(item);
    });
  },

  showAllowlistMessage(message) {
    this.elements.allowlistMessage.textContent = message;
  },

  toggleProtection() {
    if (this.state.protectionActive) {
      this.stopProtection();
      return;
    }

    this.showMessage(
      "MGA VPN koruma motoru henüz hazır değil. " +
      "Cihaz güvenliği için koruma başlatılamaz."
    );
  },

  startProtection() {
    console.warn("MGA VPNService is not integrated yet.");
  },

  stopProtection() {
    console.warn("MGA VPNService is not integrated yet.");
  },

  updateInterface() {
    this.elements.blockedCount.textContent =
      this.state.blockedCount;

    this.elements.checkedCount.textContent =
      this.state.checkedCount;

    this.elements.protectionStatus.textContent =
      "Henüz başlatılmadı";

    this.elements.protectionDescription.textContent =
      "Koruma motoru henüz etkin değil.";

    this.elements.protectionButton.textContent =
      "Korumayı başlat";

    this.elements.shieldIcon.textContent = "🛡️";

    this.updateLogsScreen();
    this.updateAllowlistScreen();
  },

  showComingSoon(feature) {
    this.showMessage(
      feature + " özelliği henüz geliştirme aşamasında."
    );
  },

  showMessage(message) {
    window.alert(message);
  }
};

document.addEventListener("DOMContentLoaded", () => {
  MGA.init();
});
