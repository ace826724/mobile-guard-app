"use strict";

/*
 * MGA - Mobile Guard App
 * Main JavaScript Controller
 * Version: 1.0.0
 *
 * Engelleme kayıtları ekranı ve ana ekran yönetimi.
 * VPN motoru henüz entegre edilmemiştir.
 */

const MGA = {
  version: "1.0.0",

  state: {
    protectionActive: false,
    blockedCount: 0,
    checkedCount: 0,
    logs: []
  },

  elements: {},

  init() {
    this.cacheElements();
    this.bindEvents();
    this.updateInterface();
    this.showHomeScreen();

    console.log("MGA Mobile Guard App initialized.");
  },

  cacheElements() {
    this.elements = {
      homeScreen: document.getElementById("homeScreen"),
      logsScreen: document.getElementById("logsScreen"),

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
      () => this.showComingSoon("Güvenilir siteler")
    );

    this.elements.settingsButton.addEventListener(
      "click",
      () => this.showComingSoon("Ayarlar")
    );
  },

  showHomeScreen() {
    this.elements.logsScreen.hidden = true;
    this.elements.homeScreen.hidden = false;
    window.scrollTo(0, 0);
  },

  showLogsScreen() {
    this.updateLogsScreen();

    this.elements.homeScreen.hidden = true;
    this.elements.logsScreen.hidden = false;

    window.scrollTo(0, 0);
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
