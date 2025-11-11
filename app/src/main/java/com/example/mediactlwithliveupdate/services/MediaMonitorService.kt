package com.kuwa3sin.mediactlwithliveupdate.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kuwa3sin.mediactlwithliveupdate.R
import com.kuwa3sin.mediactlwithliveupdate.config.ChipIndicatorMode
import com.kuwa3sin.mediactlwithliveupdate.config.ChipIndicatorPreferences

/**
 * Live Media Control - Core Service
 *
 * このサービスは、AOSP環境下でLive Update機能を活用したメディアコントローラーを実現する。
 *
 * アーキテクチャ概要:
 * 1. NotificationListenerServiceとして他アプリのMediaStyle通知を傍受
 * 2. MediaSession.Tokenを抽出してMediaControllerを作成
 * 3. メタデータと再生状態の変更をリッスン
 * 4. StandardStyle通知をLive Updateとしてプロモート
 *
 * トレードオフ:
 * - AOSPはMediaStyleのLive Updateプロモーションを禁止しているため、StandardStyleを使用
 * - システム標準メディアコントロールとの二重表示は意図的な設計（ユーザー許容済み）
 */
class MediaMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaMonitorService"
        private const val LIVE_UPDATE_NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "media_live_update_channel"

        // アクションボタン用のIntent Action定義
    private const val ACTION_PLAY = "com.kuwa3sin.mediactlwithliveupdate.ACTION_PLAY"
    private const val ACTION_PAUSE = "com.kuwa3sin.mediactlwithliveupdate.ACTION_PAUSE"
    private const val ACTION_PREVIOUS = "com.kuwa3sin.mediactlwithliveupdate.ACTION_PREVIOUS"
    private const val ACTION_SKIP = "com.kuwa3sin.mediactlwithliveupdate.ACTION_SKIP"

        private val MEDIA_STYLE_TEMPLATES = setOf(
            "android.app.Notification\$MediaStyle",
            "android.app.Notification\$DecoratedMediaCustomViewStyle",
            "androidx.media.app.NotificationCompat\$MediaStyle",
            "androidx.media.app.NotificationCompat\$DecoratedMediaCustomViewStyle",
            "android.support.v4.media.app.NotificationCompat\$MediaStyle",
            "android.support.v4.media.app.NotificationCompat\$DecoratedMediaCustomViewStyle"
        )
    }

    // 状態管理
    private var mCurrentController: MediaController? = null
    private var mMediaCallback: MediaController.Callback? = null
    private var mLaunchIntent: PendingIntent? = null
    private var mSourceSmallIcon: Icon? = null
    private lateinit var preferences: SharedPreferences
    private var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var mChipIndicatorMode: ChipIndicatorMode = ChipIndicatorMode.APP_ICON_ONLY
    private lateinit var mNotificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaMonitorService created")
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        preferences = getSharedPreferences(ChipIndicatorPreferences.PREFS_NAME, MODE_PRIVATE)
        mChipIndicatorMode = ChipIndicatorPreferences.read(preferences)
        preferenceListener = ChipIndicatorPreferences.observe(preferences) { mode ->
            mChipIndicatorMode = mode
        }
        createNotificationChannel()
    }

    /**
     * 通知チャンネルを作成
     * Live Update通知用の専用チャンネルを設定
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        mNotificationManager.createNotificationChannel(channel)
    }

    /**
     * NotificationListenerServiceが接続されたときの処理
     * 既存のアクティブな通知をスキャンしてメディアセッションを探す
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        findActiveMediaSession()
    }

    /**
     * 新しい通知が投稿されたときの処理
     * MediaStyle通知からMediaSession.Tokenを抽出
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        processNotification(sbn)
    }

    /**
     * 通知が変更されたときの処理
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        // 監視中のメディアセッションの通知が削除された場合、コントローラーをクリーンアップ
        val notification = sbn.notification

        if (notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            val token = notification.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                MediaSession.Token::class.java
            )
            if (token == mCurrentController?.sessionToken) {
                Log.d(TAG, "Active media session notification removed, finding new session")
                findActiveMediaSession()
            }
        }
    }

    /**
     * 既存の通知からアクティブなメディアセッションを探す
     */
    private fun findActiveMediaSession() {
        val activeNotifications = activeNotifications ?: return

        for (sbn in activeNotifications) {
            if (processNotification(sbn)) {
                // アクティブなメディアセッションが見つかった
                return
            }
        }

        // アクティブなメディアセッションが見つからない場合、通知をクリア
        Log.d(TAG, "No active media session found")
        clearLiveUpdateNotification()
    }

    /**
     * 通知を処理し、MediaStyle通知の場合はMediaSession.Tokenを抽出
     * @return MediaSessionが見つかり、初期化された場合true
     */
    private fun processNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification

        if (!isMediaStyleNotification(notification)) {
            return false
        }

        // MediaSession.Tokenを含む通知かチェック
        if (!notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            return false
        }

        val token = notification.extras.getParcelable(
            Notification.EXTRA_MEDIA_SESSION,
            MediaSession.Token::class.java
        ) ?: return false

        val resolvedIcon = resolveSourceSmallIcon(sbn)
        mSourceSmallIcon = resolvedIcon

        // 既に同じセッションを監視している場合はスキップ
        if (token == mCurrentController?.sessionToken) {
            if (mLaunchIntent == null) {
                mLaunchIntent = buildLaunchIntent(sbn)
            }
            return true
        }

        Log.d(TAG, "Found MediaSession from ${sbn.packageName}")
        mLaunchIntent = buildLaunchIntent(sbn)
        initializeMediaController(token)
        return true
    }

    /**
     * MediaControllerを初期化し、コールバックを登録
     *
     * このメソッドは、MediaSession.Tokenから新しいMediaControllerを作成し、
     * メタデータと再生状態の変更を監視するコールバックを登録する。
     */
    private fun initializeMediaController(token: MediaSession.Token) {
        // 古いコントローラーのクリーンアップ
        mCurrentController?.let { controller ->
            mMediaCallback?.let { callback ->
                controller.unregisterCallback(callback)
            }
        }

        // 新しいコールバックを作成
        mMediaCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                Log.d(TAG, "Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
                updateNotification(metadata, mCurrentController?.playbackState)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.d(TAG, "Playback state changed: ${state?.state}")
                updateNotification(mCurrentController?.metadata, state)
            }
        }

        // 新しいコントローラーを作成
        mCurrentController = MediaController(applicationContext, token)

        // コールバックを登録
        mMediaCallback?.let { callback ->
            mCurrentController?.registerCallback(callback)
        }

        // 初回実行: 即座に通知を投稿
        updateNotification(mCurrentController?.metadata, mCurrentController?.playbackState)
    }

    /**
     * Live Update通知を更新
     *
     * これが本アーキテクチャの核心部分。
     * MediaMetadataとPlaybackStateを基に、StandardStyle通知を構築し、
     * setRequestPromotedOngoing(true)を呼び出してLive Updateとしてプロモートする。
     *
     * startForegroundを呼び出すことで:
     * 1. サービスをフォアグラウンド状態に維持
     * 2. 内部的にNotificationManager.notify()が呼び出される
     * 3. 同じIDで繰り返し呼び出すことで通知をアトミックに更新
     */
    private fun updateNotification(metadata: MediaMetadata?, state: PlaybackState?) {
        // 必須情報が欠落している場合は何もしない
        if (metadata == null || state == null) {
            Log.d(TAG, "Metadata or state is null, skipping notification update")
            return
        }

    val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
    val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
    val displayTitle = "\uD83C\uDFB5 $title"
    val displayArtist = "\uD83C\uDFA4 $artist"
    val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    val sourceAppName = resolveSourceAppName()

        // 通知ビルダーを作成
        val builder = Notification.Builder(this, CHANNEL_ID)
            // Live Update昇格要件
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            // UIマッピング: StandardStyleを使用（MediaStyleは使用しない）
            .setContentTitle(displayTitle)  // ステータスバーチップに表示される
            .setContentText(displayArtist)
            .setOnlyAlertOnce(true)  // 更新のたびに音が鳴らないようにする
            .setVisibility(Notification.VISIBILITY_PUBLIC)

        val baseIconRes = R.drawable.ic_stat_music
        val playbackIconRes = when (state.state) {
            PlaybackState.STATE_PLAYING -> R.drawable.ic_play
            PlaybackState.STATE_PAUSED -> R.drawable.ic_pause
            else -> null
        }

        val defaultAppIcon = Icon.createWithResource(this, baseIconRes)
        val appIcon = mSourceSmallIcon ?: defaultAppIcon
        val playbackIcon = playbackIconRes?.let { Icon.createWithResource(this, it) }
        val smallIcon = when (mChipIndicatorMode) {
            ChipIndicatorMode.APP_ICON_ONLY -> appIcon
            ChipIndicatorMode.PLAYBACK_ICON_ONLY -> playbackIcon ?: defaultAppIcon
        }
        builder.setSmallIcon(smallIcon)

        mLaunchIntent?.let { intent ->
            builder.setContentIntent(intent)
        }
        // Live Updateとしてプロモート (Android 16+)
        try {
            val method = Notification.Builder::class.java.getMethod(
                "setRequestPromotedOngoing",
                Boolean::class.javaPrimitiveType
            )
            method.invoke(builder, true)
        } catch (t: Throwable) {
            Log.w(TAG, "setRequestPromotedOngoing not available", t)
        }

        sourceAppName?.let { builder.setSubText(it) }

        setShortCriticalTextCompat(builder, null)

        // アルバムアートが存在する場合は設定
        art?.let { artBitmap ->
            builder.setLargeIcon(artBitmap)
        }

        // アクションボタンを追加
        builder.addAction(
            buildNotificationAction(
                R.drawable.ic_previous,
                getString(R.string.notification_action_previous),
                buildServicePendingIntent(ACTION_PREVIOUS, 0)
            )
        )

        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        builder.addAction(
            buildNotificationAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) {
                    getString(R.string.notification_action_pause)
                } else {
                    getString(R.string.notification_action_play)
                },
                buildServicePendingIntent(
                    if (isPlaying) ACTION_PAUSE else ACTION_PLAY,
                    1
                )
            )
        )

        builder.addAction(
            buildNotificationAction(
                R.drawable.ic_skip,
                getString(R.string.notification_action_skip),
                buildServicePendingIntent(ACTION_SKIP, 2)
            )
        )

        // 通知を構築
        val notification = builder.build()

        // サービスを永続化し、通知を投稿
        // startForegroundは内部的にNotificationManager.notifyを呼び出す
        // コールバックのたびにstartForegroundを呼び出すことで、
        // サービスを維持しつつ通知をアトミックに更新する
        try {
            startForeground(LIVE_UPDATE_NOTIFICATION_ID, notification)
            Log.d(TAG, "Live Update notification posted: $title - $artist")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }

    /**
     * 通知にアクションボタンを追加
     *
     * 3つのボタンを追加:
     * 1. 前の曲へ (skipToPrevious)
     * 2. 再生/一時停止 (動的に切り替え)
     * 3. 次の曲へ (skipToNext)
     */
    /**
     * アクションボタンからのIntentを処理
     *
     * MediaController.TransportControlsを介して、
     * 対応するメディア操作コマンドを実行する
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            val transportControls = mCurrentController?.transportControls

            when (action) {
                ACTION_PLAY -> {
                    Log.d(TAG, "Starting playback")
                    transportControls?.play()
                }
                ACTION_PAUSE -> {
                    Log.d(TAG, "Pausing playback")
                    transportControls?.pause()
                }
                ACTION_PREVIOUS -> {
                    Log.d(TAG, "Skipping to previous")
                    transportControls?.skipToPrevious()
                }
                ACTION_SKIP -> {
                    Log.d(TAG, "Skipping to next")
                    transportControls?.skipToNext()
                }
            }
        }

        return START_STICKY
    }

    /**
     * Live Update通知をクリア
     */
    private fun clearLiveUpdateNotification() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notification", e)
        }
        mLaunchIntent = null
        mSourceSmallIcon = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MediaMonitorService destroyed")

        // コールバックの解除
        mCurrentController?.let { controller ->
            mMediaCallback?.let { callback ->
                controller.unregisterCallback(callback)
            }
        }

        mCurrentController = null
        mMediaCallback = null
        preferenceListener?.let { listener ->
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
        preferenceListener = null
    }

    private fun buildServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MediaMonitorService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotificationAction(
        iconResId: Int,
        title: String,
        pendingIntent: PendingIntent
    ): Notification.Action {
        val icon = Icon.createWithResource(this, iconResId)
        return Notification.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun resolveSourceAppName(): String? {
        val packageName = mCurrentController?.packageName ?: return null
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo)?.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Failed to resolve app label for $packageName", e)
            null
        }
    }

    private fun resolveSourceSmallIcon(sbn: StatusBarNotification): Icon? {
        sbn.notification.smallIcon?.let { return it }

        return try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            if (appInfo.icon != 0) {
                Icon.createWithResource(sbn.packageName, appInfo.icon)
            } else {
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Failed to resolve icon for ${sbn.packageName}", e)
            null
        }
    }

    private fun setShortCriticalTextCompat(builder: Notification.Builder, text: CharSequence?) {
        // API仕様に従い、短く簡潔なテキストのみを許容し、不要な改行は除去する
        val sanitized = text
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace('\n', ' ')
            ?.let { raw ->
                if (raw.length <= 32) raw else raw.take(31) + "…"
            }

        builder.setShortCriticalText(sanitized)
    }

    private fun isMediaStyleNotification(notification: Notification): Boolean {
        // Notification#getStyle() is not part of the public API, so rely on the template marker.
        val template = notification.extras.getString(Notification.EXTRA_TEMPLATE) ?: return false
        return MEDIA_STYLE_TEMPLATES.contains(template)
    }

    private fun buildLaunchIntent(sbn: StatusBarNotification): PendingIntent? {
        sbn.notification.contentIntent?.let { existing ->
            return existing
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: return null

        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}

