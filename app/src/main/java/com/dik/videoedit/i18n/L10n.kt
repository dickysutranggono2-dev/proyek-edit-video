package com.dik.videoedit.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Terjemahan UI tanpa file resource: teks sumber di kode berbahasa Indonesia, dan [tr] menerjemahkannya
 * lewat tabel di bawah. Teks yang tidak ada di tabel tampil apa adanya (Indonesia); bila kolom bahasa
 * kosong, dipakai bahasa Inggris.
 */
object L10n {
    class Lang(val code: String, val native: String)

    val languages = listOf(
        Lang("id", "Bahasa Indonesia"), Lang("en", "English"), Lang("ms", "Bahasa Melayu"),
        Lang("es", "Español"), Lang("pt", "Português"), Lang("fr", "Français"),
        Lang("de", "Deutsch"), Lang("ja", "日本語"), Lang("ko", "한국어"), Lang("ru", "Русский"),
    )

    private const val PREFS = "videoedit_lang"

    @Volatile var code: String = "id"
        private set

    private var column by mutableIntStateOf(0) // 0 = Indonesia (tanpa terjemahan); state agar UI ikut berganti
    private val table: Map<String, List<String>> by lazy { parse(TABLE) }

    /** Panggil sekali saat aplikasi mulai. Tanpa pilihan tersimpan, ikuti bahasa perangkat bila didukung. */
    fun init(context: Context) {
        val saved = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("code", null) }.getOrNull()
        val device = Locale.getDefault().language.let { if (it == "in") "id" else it }
        select(saved ?: device.takeIf { d -> languages.any { it.code == d } } ?: "id")
    }

    fun setLanguage(context: Context, newCode: String) {
        runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("code", newCode).apply() }
        select(newCode)
    }

    private fun select(c: String) {
        val i = languages.indexOfFirst { it.code == c }.let { if (it < 0) 0 else it }
        code = languages[i].code
        column = i
    }

    fun tr(s: String): String {
        if (column == 0 || s.isEmpty()) return s
        val row = table[s] ?: return s
        // row[0] = Inggris, row[1] = Melayu, dst. (kolom bahasa = indeks daftar - 1)
        val v = row.getOrNull(column - 1)?.takeIf { it.isNotBlank() }
        return v ?: row.getOrNull(0)?.takeIf { it.isNotBlank() } ?: s
    }

    /** Teks panjang (kebijakan privasi, dll.): bahasa Inggris untuk selain Indonesia. */
    fun long(id: String, indonesian: String): String =
        if (column == 0) indonesian else LONG[id] ?: indonesian

    private fun parse(raw: String): Map<String, List<String>> {
        val map = HashMap<String, List<String>>()
        raw.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split(" | ").map { it.trim().replace("\\n", "\n") }
            if (parts.size >= 2) map[parts[0]] = parts.drop(1)
        }
        return map
    }

    private val LONG = mapOf(
        "privacy" to """VideoEdit processes your videos entirely on your device. Your videos, music and projects are never uploaded to our servers.

Data stored on the device: the project list (name, edit settings and source file addresses) and app settings. You can delete it at any time by deleting projects or the app data.

Ads: the app shows rewarded ads through Google AdMob. AdMob may collect data such as the advertising ID and device information under Google's policy. Ads are only loaded when you choose to unlock premium features.

Permissions: internet access (for ads only), notifications (to tell you when an export is done), and storage on Android 9 and below (to save exports to the gallery).""",
        "terms" to """By using VideoEdit you agree that:

1. You are responsible for the content you edit and share, including the copyright of the videos and music you use.
2. Premium features are unlocked temporarily (30 minutes) after you watch one ad to the end.
3. The app is provided as is. Keep a copy of your original videos before editing.
4. Exported videos are saved to the gallery in the Movies/VideoEdit folder.""",
    )

    // Format tiap baris: teks Indonesia | Inggris | Melayu | Spanyol | Portugis | Prancis | Jerman | Jepang | Korea | Rusia
    private const val T0 = """
Buka Fitur Premium | Unlock Premium Features | Buka Ciri Premium | Desbloquear funciones premium | Desbloquear recursos premium | Débloquer les fonctions premium | Premium-Funktionen freischalten | プレミアム機能を解放 | 프리미엄 기능 잠금 해제 | Открыть премиум-функции
Tonton iklan untuk membuka fitur premium berikut selama 30 menit: | Watch an ad to unlock these premium features for 30 minutes: | Tonton iklan untuk membuka ciri premium ini selama 30 minit: | Mira un anuncio para desbloquear estas funciones premium durante 30 minutos: | Assista a um anúncio para desbloquear estes recursos premium por 30 minutos: | Regardez une pub pour débloquer ces fonctions premium pendant 30 minutes : | Sieh dir eine Werbung an, um diese Premium-Funktionen 30 Minuten lang freizuschalten: | 広告を見て、以下のプレミアム機能を30分間解放します: | 광고를 시청하면 아래 프리미엄 기능을 30분간 사용할 수 있습니다: | Посмотрите рекламу, чтобы открыть эти премиум-функции на 30 минут:
Tidak dapat menampilkan iklan. | Could not show the ad. | Tidak dapat memaparkan iklan. | No se pudo mostrar el anuncio. | Não foi possível exibir o anúncio. | Impossible d'afficher la pub. | Werbung konnte nicht angezeigt werden. | 広告を表示できませんでした。 | 광고를 표시할 수 없습니다. | Не удалось показать рекламу.
Tambah audio | Add audio | Tambah audio | Añadir audio | Adicionar áudio | Ajouter un audio | Audio hinzufügen | 音声を追加 | 오디오 추가 | Добавить аудио
Crop & Rotasi | Crop & Rotate | Pangkas & Putar | Recortar y rotar | Cortar e girar | Recadrer et pivoter | Zuschneiden & Drehen | トリミングと回転 | 자르기 및 회전 | Кадрирование и поворот
Rasio | Ratio | Nisbah | Proporción | Proporção | Format | Format | 比率 | 비율 | Формат
Putar 90° | Rotate 90° | Putar 90° | Girar 90° | Girar 90° | Pivoter de 90° | 90° drehen | 90°回転 | 90° 회전 | Повернуть на 90°
Kecepatan | Speed | Kelajuan | Velocidad | Velocidade | Vitesse | Geschwindigkeit | 速度 | 속도 | Скорость
Slow motion | Slow motion | Gerak perlahan | Cámara lenta | Câmera lenta | Ralenti | Zeitlupe | スローモーション | 슬로 모션 | Замедление
Speed ramp | Speed ramp | Speed ramp | Rampa de velocidad | Rampa de velocidade | Rampe de vitesse | Geschwindigkeitsrampe | スピードランプ | 스피드 램프 | Изменение скорости
Volume Audio | Audio Volume | Kelantangan Audio | Volumen de audio | Volume do áudio | Volume audio | Audiolautstärke | 音量 | 오디오 볼륨 | Громкость аудио
Fade Audio | Audio Fade | Pudar Audio | Fundido de audio | Fade do áudio | Fondu audio | Audio-Überblendung | 音声フェード | 오디오 페이드 | Затухание звука
Fade in | Fade in | Pudar masuk | Entrada gradual | Fade in | Fondu entrant | Einblenden | フェードイン | 페이드 인 | Появление
Fade out | Fade out | Pudar keluar | Salida gradual | Fade out | Fondu sortant | Ausblenden | フェードアウト | 페이드 아웃 | Затухание
Kurangi bising | Reduce noise | Kurangkan bunyi bising | Reducir ruido | Reduzir ruído | Réduire le bruit | Rauschen reduzieren | ノイズ低減 | 노이즈 감소 | Шумоподавление
Teks | Text | Teks | Texto | Texto | Texte | Text | テキスト | 텍스트 | Текст
Isi teks (maks. 30 huruf) | Text (max. 30 characters) | Isi teks (maks. 30 huruf) | Texto (máx. 30 caracteres) | Texto (máx. 30 caracteres) | Texte (30 caractères max.) | Text (max. 30 Zeichen) | テキスト(最大30文字) | 텍스트 (최대 30자) | Текст (до 30 символов)
Posisi | Position | Kedudukan | Posición | Posição | Position | Position | 位置 | 위치 | Положение
Ukuran | Size | Saiz | Tamaño | Tamanho | Taille | Größe | サイズ | 크기 | Размер
Warna | Color | Warna | Color | Cor | Couleur | Farbe | 色 | 색상 | Цвет
Teks di video | Texts in video | Teks dalam video | Textos en el vídeo | Textos no vídeo | Textes dans la vidéo | Texte im Video | 動画内のテキスト | 영상 속 텍스트 | Тексты в видео
Stiker & Overlay | Stickers & Overlay | Pelekat & Tindanan | Pegatinas y superposición | Figurinhas e overlay | Stickers et incrustation | Sticker & Overlay | ステッカーとオーバーレイ | 스티커 및 오버레이 | Стикеры и наложение
Pilih stiker | Choose a sticker | Pilih pelekat | Elige una pegatina | Escolha uma figurinha | Choisir un sticker | Sticker wählen | ステッカーを選択 | 스티커 선택 | Выберите стикер
Tambah Stiker | Add Sticker | Tambah Pelekat | Añadir pegatina | Adicionar figurinha | Ajouter le sticker | Sticker hinzufügen | ステッカーを追加 | 스티커 추가 | Добавить стикер
Overlay Foto | Photo Overlay | Tindanan Foto | Foto superpuesta | Foto overlay | Photo en incrustation | Foto-Overlay | 写真オーバーレイ | 사진 오버레이 | Фото поверх видео
Stiker & overlay di video | Stickers & overlays in video | Pelekat & tindanan dalam video | Pegatinas y superposiciones en el vídeo | Figurinhas e overlays no vídeo | Stickers et incrustations dans la vidéo | Sticker & Overlays im Video | 動画内のステッカーとオーバーレイ | 영상 속 스티커 및 오버레이 | Стикеры и наложения в видео
Animasi | Animation | Animasi | Animación | Animação | Animation | Animation | アニメーション | 애니메이션 | Анимация
Reset semua | Reset all | Tetap semula semua | Restablecer todo | Redefinir tudo | Tout réinitialiser | Alles zurücksetzen | すべてリセット | 모두 초기화 | Сбросить всё
Efek & Transisi | Effects & Transitions | Kesan & Peralihan | Efectos y transiciones | Efeitos e transições | Effets et transitions | Effekte & Übergänge | エフェクトとトランジション | 효과 및 전환 | Эффекты и переходы
Efek video | Video effect | Kesan video | Efecto de vídeo | Efeito de vídeo | Effet vidéo | Videoeffekt | ビデオエフェクト | 비디오 효과 | Эффект видео
Transisi antar klip | Transition between clips | Peralihan antara klip | Transición entre clips | Transição entre clipes | Transition entre les clips | Übergang zwischen Clips | クリップ間のトランジション | 클립 간 전환 | Переход между клипами
Keterangan | Captions | Kapsyen | Subtítulos | Legendas | Sous-titres | Untertitel | キャプション | 자막 | Субтитры
Template | Template | Templat | Plantilla | Modelo | Modèle | Vorlage | テンプレート | 템플릿 | Шаблон
Isi keterangan | Caption text | Teks kapsyen | Texto del subtítulo | Texto da legenda | Texte du sous-titre | Untertiteltext | キャプションのテキスト | 자막 내용 | Текст субтитра
Lama tampil | Duration | Tempoh paparan | Duración | Duração | Durée d'affichage | Anzeigedauer | 表示時間 | 표시 시간 | Длительность
Tambah di posisi sekarang | Add at current position | Tambah pada kedudukan semasa | Añadir en la posición actual | Adicionar na posição atual | Ajouter à la position actuelle | An aktueller Position hinzufügen | 現在位置に追加 | 현재 위치에 추가 | Добавить в текущей позиции
Impor keterangan (.srt / .vtt) | Import captions (.srt / .vtt) | Import kapsyen (.srt / .vtt) | Importar subtítulos (.srt / .vtt) | Importar legendas (.srt / .vtt) | Importer des sous-titres (.srt / .vtt) | Untertitel importieren (.srt / .vtt) | キャプションを読み込む (.srt / .vtt) | 자막 가져오기 (.srt / .vtt) | Импорт субтитров (.srt / .vtt)
Klip dibagi otomatis menjadi beberapa bagian dengan kecepatan berbeda (minimal 3 detik). | The clip is split automatically into parts with different speeds (at least 3 seconds). | Klip dibahagi secara automatik kepada beberapa bahagian dengan kelajuan berbeza (minimum 3 saat). | El clip se divide automáticamente en partes con distinta velocidad (mínimo 3 segundos). | O clipe é dividido automaticamente em partes com velocidades diferentes (mínimo 3 segundos). | Le clip est découpé automatiquement en parties de vitesses différentes (3 secondes minimum). | Der Clip wird automatisch in Teile mit unterschiedlicher Geschwindigkeit geteilt (mindestens 3 Sekunden).
Membuang dengung rendah dan desis pelan. Berlaku untuk semua audio dan terdengar pada hasil export. | Removes low hum and faint hiss. Applies to all audio and is heard in the exported video. | Membuang dengung rendah dan desis perlahan. Terpakai pada semua audio dan kedengaran dalam hasil eksport. | Elimina el zumbido grave y el siseo suave. Se aplica a todo el audio y se oye en el vídeo exportado. | Remove o zumbido grave e o chiado leve. Vale para todo o áudio e é ouvido no vídeo exportado. | Supprime le bourdonnement grave et le souffle léger. S'applique à tout l'audio et s'entend dans la vidéo exportée. | Entfernt tiefes Brummen und leises Rauschen. Gilt für alle Audiospuren und ist im exportierten Video hörbar.
Maksimal 12 teks, stiker, dan overlay. | Maximum 12 texts, stickers and overlays. | Maksimum 12 teks, pelekat dan tindanan. | Máximo 12 textos, pegatinas y superposiciones. | Máximo de 12 textos, figurinhas e overlays. | 12 textes, stickers et incrustations maximum. | Maximal 12 Texte, Sticker und Overlays.
Transisi terlihat pada hasil export. Preview menampilkan potongan langsung. | Transitions show in the exported video. The preview shows direct cuts. | Peralihan kelihatan pada hasil eksport. Pratonton memaparkan potongan terus. | Las transiciones se ven en el vídeo exportado. La vista previa muestra cortes directos. | As transições aparecem no vídeo exportado. A prévia mostra cortes diretos. | Les transitions apparaissent dans la vidéo exportée. L'aperçu montre des coupes franches. | Übergänge sind im exportierten Video sichtbar. Die Vorschau zeigt harte Schnitte.
Keterangan otomatis dari suara belum tersedia karena butuh layanan online. Ketik manual, atau impor file .srt dari layanan lain. | Automatic captions from speech are not available because they need an online service. Type them manually, or import an .srt file made elsewhere.
Video diisi penuh ke rasio yang dipilih (bagian tepi dipotong). | The video fills the chosen ratio completely (edges are cropped). | Video memenuhi nisbah yang dipilih sepenuhnya (bahagian tepi dipotong). | El vídeo llena por completo la proporción elegida (se recortan los bordes). | O vídeo preenche totalmente a proporção escolhida (as bordas são cortadas). | La vidéo remplit entièrement le format choisi (les bords sont rognés). | Das Video füllt das gewählte Format komplett aus (Ränder werden beschnitten).
Teks tampil sepanjang video. | Text is shown for the whole video. | Teks dipaparkan sepanjang video. | El texto se muestra durante todo el vídeo. | O texto aparece durante todo o vídeo. | Le texte s'affiche pendant toute la vidéo. | Der Text wird während des gesamten Videos angezeigt.
Tambah emoji atau foto dari galeri di atas video. | Add emoji or photos from the gallery on top of the video. | Tambah emoji atau foto dari galeri di atas video. | Añade emojis o fotos de la galería sobre el vídeo. | Adicione emojis ou fotos da galeria sobre o vídeo. | Ajoutez des emojis ou des photos de la galerie sur la vidéo. | Füge Emojis oder Fotos aus der Galerie über dem Video hinzu.
"""

    private const val T1 = """
Gerakan zoom atau geser otomatis. Terlihat di preview dan hasil export. | Automatic zoom or pan movement. Visible in the preview and the exported video. | Gerakan zum atau gelongsor automatik. Kelihatan dalam pratonton dan hasil eksport. | Movimiento automático de zoom o desplazamiento. Se ve en la vista previa y en el vídeo exportado. | Movimento automático de zoom ou deslocamento. Visível na prévia e no vídeo exportado. | Mouvement automatique de zoom ou de panoramique. Visible dans l'aperçu et la vidéo exportée. | Automatische Zoom- oder Schwenkbewegung. In Vorschau und Export sichtbar.
Teks yang tampil pada waktu tertentu (subtitle). Geser garis putih ke posisi awal, lalu tambahkan. | Text shown at a specific time (subtitles). Move the white line to the start position, then add it. | Teks yang dipaparkan pada masa tertentu (sarikata). Seret garis putih ke kedudukan mula, kemudian tambah. | Texto que aparece en un momento concreto (subtítulos). Mueve la línea blanca al inicio y añádelo. | Texto exibido em um momento específico (legendas). Mova a linha branca até o início e adicione. | Texte affiché à un moment précis (sous-titres). Placez la ligne blanche au début, puis ajoutez-le. | Text, der zu einer bestimmten Zeit erscheint (Untertitel). Schiebe die weiße Linie zum Anfang und füge ihn hinzu.
Suara asli video. Atur ke 0% untuk membisukan. | The video's original sound. Set to 0% to mute. | Bunyi asal video. Tetapkan 0% untuk menyenyapkan. | El sonido original del vídeo. Pon 0 % para silenciar. | O som original do vídeo. Defina 0% para silenciar. | Le son d'origine de la vidéo. Réglez sur 0 % pour couper le son. | Der Originalton des Videos. Auf 0 % stellen zum Stummschalten.
Proyek tidak dapat dibuka. Video sumbernya mungkin sudah dihapus atau dipindahkan. | The project cannot be opened. Its source video may have been deleted or moved. | Projek tidak dapat dibuka. Video sumbernya mungkin telah dipadam atau dipindahkan. | No se puede abrir el proyecto. Puede que el vídeo original se haya borrado o movido. | Não foi possível abrir o projeto. O vídeo de origem pode ter sido excluído ou movido. | Impossible d'ouvrir le projet. La vidéo source a peut-être été supprimée ou déplacée. | Das Projekt kann nicht geöffnet werden. Das Quellvideo wurde möglicherweise gelöscht oder verschoben.
Kembali | Back | Kembali | Atrás | Voltar | Retour | Zurück | 戻る | 뒤로 | Назад
Export | Export | Eksport | Exportar | Exportar | Exporter | Exportieren | 書き出し | 내보내기 | Экспорт
Pisah | Split | Pisah | Dividir | Dividir | Diviser | Teilen | 分割 | 분할 | Разделить
Volume | Volume | Kelantangan | Volumen | Volume | Volume | Lautstärke | 音量 | 볼륨 | Громкость
Ekstrak Audio | Extract Audio | Ekstrak Audio | Extraer audio | Extrair áudio | Extraire l'audio | Audio extrahieren | 音声を抽出 | 오디오 추출 | Извлечь аудио
Mundur | Move back | Undur | Mover atrás | Mover para trás | Reculer | Zurück schieben | 前へ移動 | 앞으로 이동 | Назад
Maju | Move forward | Maju | Mover adelante | Mover para frente | Avancer | Vor schieben | 後ろへ移動 | 뒤로 이동 | Вперёд
Hapus | Delete | Padam | Eliminar | Excluir | Supprimer | Löschen | 削除 | 삭제 | Удалить
Fade | Fade | Pudar | Fundido | Fade | Fondu | Blende | フェード | 페이드 | Затухание
Duplikat | Duplicate | Duplikasi | Duplicar | Duplicar | Dupliquer | Duplizieren | 複製 | 복제 | Дублировать
Potong | Trim | Potong | Recortar | Cortar | Couper | Kürzen | トリム | 자르기 | Обрезать
Musik | Music | Muzik | Música | Música | Musique | Musik | 音楽 | 음악 | Музыка
Dari Video | From Video | Dari Video | Desde vídeo | De vídeo | Depuis une vidéo | Aus Video | 動画から | 영상에서 | Из видео
Crop | Crop | Pangkas | Recorte | Corte | Recadrer | Zuschnitt | 切り抜き | 자르기 | Кадр
Stiker | Stickers | Pelekat | Pegatinas | Figurinhas | Stickers | Sticker | ステッカー | 스티커 | Стикеры
Filter | Filter | Penapis | Filtro | Filtro | Filtre | Filter | フィルター | 필터 | Фильтр
Sesuaikan | Adjust | Laraskan | Ajustar | Ajustar | Ajuster | Anpassen | 調整 | 조정 | Настройка
Efek | Effects | Kesan | Efectos | Efeitos | Effets | Effekte | エフェクト | 효과 | Эффекты
Proyek tidak ditemukan. | Project not found. | Projek tidak dijumpai. | Proyecto no encontrado. | Projeto não encontrado. | Projet introuvable. | Projekt nicht gefunden. | プロジェクトが見つかりません。 | 프로젝트를 찾을 수 없습니다. | Проект не найден.
Batalkan export? | Cancel export? | Batalkan eksport? | ¿Cancelar la exportación? | Cancelar a exportação? | Annuler l'export ? | Export abbrechen? | 書き出しをキャンセルしますか? | 내보내기를 취소할까요? | Отменить экспорт?
Proses yang berjalan akan dihentikan. | The running process will be stopped. | Proses yang sedang berjalan akan dihentikan. | Se detendrá el proceso en curso. | O processo em andamento será interrompido. | Le traitement en cours sera arrêté. | Der laufende Vorgang wird abgebrochen. | 実行中の処理は停止されます。 | 진행 중인 작업이 중단됩니다. | Текущий процесс будет остановлен.
Batalkan Export | Cancel Export | Batalkan Eksport | Cancelar exportación | Cancelar exportação | Annuler l'export | Export abbrechen | 書き出しをキャンセル | 내보내기 취소 | Отменить экспорт
Lanjutkan | Continue | Teruskan | Continuar | Continuar | Continuer | Fortsetzen | 続ける | 계속 | Продолжить
Export Video | Export Video | Eksport Video | Exportar vídeo | Exportar vídeo | Exporter la vidéo | Video exportieren | 動画を書き出す | 동영상 내보내기 | Экспорт видео
Resolusi | Resolution | Resolusi | Resolución | Resolução | Résolution | Auflösung | 解像度 | 해상도 | Разрешение
Frame rate (FPS) | Frame rate (FPS) | Kadar bingkai (FPS) | Fotogramas por segundo (FPS) | Taxa de quadros (FPS) | Fréquence d'images (FPS) | Bildrate (FPS) | フレームレート (FPS) | 프레임 속도 (FPS) | Частота кадров (FPS)
Hanya opsi yang didukung perangkat dan tidak melebihi FPS video asli. | Only options supported by the device and not above the original video's FPS. | Hanya pilihan yang disokong peranti dan tidak melebihi FPS video asal. | Solo opciones compatibles con el dispositivo y sin superar los FPS del vídeo original. | Apenas opções compatíveis com o aparelho e sem ultrapassar os FPS do vídeo original. | Uniquement les options prises en charge par l'appareil, sans dépasser les FPS de la vidéo d'origine. | Nur vom Gerät unterstützte Optionen, nicht über der FPS des Originalvideos.
Hapus watermark | Remove watermark | Buang tera air | Quitar marca de agua | Remover marca d'água | Supprimer le filigrane | Wasserzeichen entfernen | ウォーターマークを削除 | 워터마크 제거 | Убрать водяной знак
Proyek ini memakai filter/efek/transisi premium. Anda akan diminta menonton iklan bila akses premium belum aktif. | This project uses premium filters/effects/transitions. You will be asked to watch an ad if premium access is not active.
Mengekspor Video... | Exporting video... | Mengeksport video... | Exportando vídeo... | Exportando vídeo... | Export de la vidéo... | Video wird exportiert... | 動画を書き出し中... | 동영상 내보내는 중... | Экспорт видео...
Mohon tunggu, proses ini mungkin memakan waktu sebentar.\nJangan tutup aplikasi selama export berjalan. | Please wait, this may take a moment.\nDo not close the app while exporting. | Sila tunggu, proses ini mungkin mengambil sedikit masa.\nJangan tutup aplikasi semasa eksport. | Espera, esto puede tardar un momento.\nNo cierres la app durante la exportación. | Aguarde, isso pode levar um momento.\nNão feche o app durante a exportação. | Veuillez patienter, cela peut prendre un moment.\nNe fermez pas l'appli pendant l'export. | Bitte warten, das kann einen Moment dauern.\nApp während des Exports nicht schließen. | しばらくお待ちください。\n書き出し中はアプリを閉じないでください。 | 잠시만 기다려 주세요.\n내보내는 동안 앱을 닫지 마세요. | Подождите, это может занять некоторое время.\nНе закрывайте приложение во время экспорта.
Izin penyimpanan diperlukan untuk menyimpan ke galeri. | Storage permission is needed to save to the gallery. | Kebenaran storan diperlukan untuk menyimpan ke galeri. | Se necesita permiso de almacenamiento para guardar en la galería. | É necessária permissão de armazenamento para salvar na galeria. | L'autorisation de stockage est nécessaire pour enregistrer dans la galerie. | Speicherberechtigung ist zum Speichern in der Galerie nötig.
Tidak ada aplikasi untuk membuka video. | No app available to open the video. | Tiada aplikasi untuk membuka video. | No hay ninguna app para abrir el vídeo. | Não há app para abrir o vídeo. | Aucune appli pour ouvrir la vidéo. | Keine App zum Öffnen des Videos.
Edit video dengan mudah\ndan cepat! | Edit videos easily\nand fast! | Sunting video dengan mudah\ndan pantas! | ¡Edita vídeos fácil\ny rápido! | Edite vídeos de forma\nfácil e rápida! | Montez vos vidéos\nsimplement et vite ! | Videos einfach\nund schnell bearbeiten! | 簡単・スピーディーに\n動画編集! | 쉽고 빠르게\n영상 편집! | Монтируйте видео\nлегко и быстро!
Proyek Terakhir | Recent Projects | Projek Terkini | Proyectos recientes | Projetos recentes | Projets récents | Letzte Projekte | 最近のプロジェクト | 최근 프로젝트 | Недавние проекты
Belum ada proyek. Tekan "Proyek Baru" untuk memilih video dari galeri. | No projects yet. Tap "New Project" to pick a video from the gallery. | Belum ada projek. Tekan "Projek Baru" untuk memilih video dari galeri. | Aún no hay proyectos. Toca "Nuevo proyecto" para elegir un vídeo de la galería. | Ainda não há projetos. Toque em "Novo projeto" para escolher um vídeo da galeria. | Aucun projet. Appuyez sur « Nouveau projet » pour choisir une vidéo de la galerie. | Noch keine Projekte. Tippe auf „Neues Projekt“, um ein Video aus der Galerie zu wählen.
Membaca video… | Reading video… | Membaca video… | Leyendo vídeo… | Lendo vídeo… | Lecture de la vidéo… | Video wird gelesen… | 動画を読み込み中… | 영상 읽는 중… | Чтение видео…
Ganti Nama Proyek | Rename Project | Namakan Semula Projek | Renombrar proyecto | Renomear projeto | Renommer le projet | Projekt umbenennen | プロジェクト名を変更 | 프로젝트 이름 변경 | Переименовать проект
Ganti Nama | Rename | Namakan Semula | Renombrar | Renomear | Renommer | Umbenennen | 名前を変更 | 이름 변경 | Переименовать
Simpan | Save | Simpan | Guardar | Salvar | Enregistrer | Speichern | 保存 | 저장 | Сохранить
Batal | Cancel | Batal | Cancelar | Cancelar | Annuler | Abbrechen | キャンセル | 취소 | Отмена
Hapus proyek? | Delete project? | Padam projek? | ¿Eliminar proyecto? | Excluir projeto? | Supprimer le projet ? | Projekt löschen? | プロジェクトを削除しますか? | 프로젝트를 삭제할까요? | Удалить проект?
Beranda | Home | Laman Utama | Inicio | Início | Accueil | Start | ホーム | 홈 | Главная
Pengaturan | Settings | Tetapan | Ajustes | Configurações | Réglages | Einstellungen | 設定 | 설정 | Настройки
"""

    private const val T2 = """
Umum | General | Umum | General | Geral | Général | Allgemein | 一般 | 일반 | Общие
Kualitas Video Default | Default Video Quality | Kualiti Video Lalai | Calidad de vídeo predeterminada | Qualidade de vídeo padrão | Qualité vidéo par défaut | Standard-Videoqualität | デフォルトの動画品質 | 기본 영상 화질 | Качество видео по умолчанию
Tema Aplikasi | App Theme | Tema Aplikasi | Tema de la app | Tema do app | Thème de l'appli | App-Design | アプリのテーマ | 앱 테마 | Тема приложения
Bahasa | Language | Bahasa | Idioma | Idioma | Langue | Sprache | 言語 | 언어 | Язык
Notifikasi | Notifications | Pemberitahuan | Notificaciones | Notificações | Notifications | Benachrichtigungen | 通知 | 알림 | Уведомления
Tentang | About | Perihal | Acerca de | Sobre | À propos | Info | アプリについて | 정보 | О приложении
Versi Aplikasi | App Version | Versi Aplikasi | Versión de la app | Versão do app | Version de l'appli | App-Version | アプリのバージョン | 앱 버전 | Версия приложения
Kebijakan Privasi | Privacy Policy | Dasar Privasi | Política de privacidad | Política de privacidade | Politique de confidentialité | Datenschutz | プライバシーポリシー | 개인정보 처리방침 | Политика конфиденциальности
Syarat & Ketentuan | Terms & Conditions | Terma & Syarat | Términos y condiciones | Termos e condições | Conditions d'utilisation | Nutzungsbedingungen | 利用規約 | 이용 약관 | Условия использования
Hubungi Kami | Contact Us | Hubungi Kami | Contáctanos | Fale conosco | Nous contacter | Kontakt | お問い合わせ | 문의하기 | Связаться с нами
Tutup | Close | Tutup | Cerrar | Fechar | Fermer | Schließen | 閉じる | 닫기 | Закрыть
Terang | Light | Cerah | Claro | Claro | Clair | Hell | ライト | 라이트 | Светлая
Gelap | Dark | Gelap | Oscuro | Escuro | Sombre | Dunkel | ダーク | 다크 | Тёмная
Sistem | System | Sistem | Sistema | Sistema | Système | System | システム | 시스템 | Системная
Ikuti Sistem | Follow System | Ikut Sistem | Seguir el sistema | Seguir o sistema | Suivre le système | System folgen | システムに合わせる | 시스템 설정 따르기 | Как в системе
Dibuat oleh | Made by | Dibuat oleh | Hecho por | Feito por | Créé par | Erstellt von | 作者 | 제작 | Автор
Izin notifikasi ditolak. | Notification permission denied. | Kebenaran pemberitahuan ditolak. | Permiso de notificaciones denegado. | Permissão de notificações negada. | Autorisation des notifications refusée. | Benachrichtigungsberechtigung abgelehnt. | 通知の許可が拒否されました。 | 알림 권한이 거부되었습니다. | Разрешение на уведомления отклонено.
Tidak ada aplikasi email terpasang. | No email app installed. | Tiada aplikasi e-mel dipasang. | No hay ninguna app de correo instalada. | Nenhum app de e-mail instalado. | Aucune appli de messagerie installée. | Keine E-Mail-App installiert. | メールアプリがインストールされていません。 | 설치된 이메일 앱이 없습니다. | Почтовое приложение не установлено.
Pilih gaya awal, lalu pilih video dari galeri. Semua pengaturan masih bisa diubah di editor. | Pick a starting style, then choose videos from the gallery. Everything can still be changed in the editor. | Pilih gaya permulaan, kemudian pilih video dari galeri. Semua tetapan masih boleh diubah dalam editor. | Elige un estilo inicial y luego vídeos de la galería. Todo se puede cambiar en el editor. | Escolha um estilo inicial e depois vídeos da galeria. Tudo ainda pode ser alterado no editor. | Choisissez un style de départ, puis des vidéos de la galerie. Tout reste modifiable dans l'éditeur. | Wähle einen Startstil und dann Videos aus der Galerie. Alles lässt sich im Editor noch ändern.
Geser posisi putar ke tengah klip dulu, lalu tekan Pisah. | Move the playhead to the middle of the clip first, then tap Split. | Seret kedudukan main ke tengah klip dahulu, kemudian tekan Pisah. | Mueve el cabezal al centro del clip y pulsa Dividir. | Mova o cursor para o meio do clipe e toque em Dividir. | Placez d'abord la tête de lecture au milieu du clip, puis appuyez sur Diviser. | Schiebe den Abspielkopf zuerst in die Mitte des Clips und tippe dann auf Teilen.
Proyek harus memiliki minimal satu klip. | A project must have at least one clip. | Projek mesti mempunyai sekurang-kurangnya satu klip. | Un proyecto debe tener al menos un clip. | Um projeto precisa ter pelo menos um clipe. | Un projet doit contenir au moins un clip. | Ein Projekt braucht mindestens einen Clip.
File tidak dapat dibuka. | The file cannot be opened. | Fail tidak dapat dibuka. | No se puede abrir el archivo. | Não foi possível abrir o arquivo. | Impossible d'ouvrir le fichier. | Die Datei kann nicht geöffnet werden.
File ini tidak memiliki suara. | This file has no sound. | Fail ini tiada bunyi. | Este archivo no tiene sonido. | Este arquivo não tem som. | Ce fichier n'a pas de son. | Diese Datei hat keinen Ton.
Suara dari video ditambahkan sebagai track audio. | The video's sound was added as an audio track. | Bunyi daripada video ditambah sebagai trek audio. | El sonido del vídeo se añadió como pista de audio. | O som do vídeo foi adicionado como faixa de áudio. | Le son de la vidéo a été ajouté comme piste audio. | Der Ton des Videos wurde als Audiospur hinzugefügt.
Klip ini tidak memiliki suara untuk diekstrak. | This clip has no sound to extract. | Klip ini tiada bunyi untuk diekstrak. | Este clip no tiene sonido para extraer. | Este clipe não tem som para extrair. | Ce clip n'a pas de son à extraire. | Dieser Clip hat keinen Ton zum Extrahieren.
Audio diekstrak ke track audio. Suara asli klip dibisukan. | Audio extracted to an audio track. The clip's original sound is muted. | Audio diekstrak ke trek audio. Bunyi asal klip disenyapkan. | Audio extraído a una pista de audio. El sonido original del clip se silenció. | Áudio extraído para uma faixa de áudio. O som original do clipe foi silenciado. | Audio extrait vers une piste audio. Le son d'origine du clip est coupé. | Audio in eine Audiospur extrahiert. Der Originalton des Clips ist stummgeschaltet.
Geser playhead ke bagian tengah klip audio dulu, lalu tekan Pisah. | Move the playhead to the middle of the audio clip first, then tap Split. | Seret playhead ke tengah klip audio dahulu, kemudian tekan Pisah. | Mueve el cabezal al centro del clip de audio y pulsa Dividir. | Mova o cursor para o meio do clipe de áudio e toque em Dividir. | Placez d'abord la tête de lecture au milieu du clip audio, puis appuyez sur Diviser. | Schiebe den Abspielkopf zuerst in die Mitte des Audioclips und tippe dann auf Teilen.
Gambar tidak dapat dibuka. | The image cannot be opened. | Imej tidak dapat dibuka. | No se puede abrir la imagen. | Não foi possível abrir a imagem. | Impossible d'ouvrir l'image. | Das Bild kann nicht geöffnet werden.
File keterangan tidak terbaca. Gunakan file .srt atau .vtt. | The caption file could not be read. Use an .srt or .vtt file. | Fail kapsyen tidak dapat dibaca. Gunakan fail .srt atau .vtt. | No se pudo leer el archivo de subtítulos. Usa un archivo .srt o .vtt. | Não foi possível ler o arquivo de legendas. Use um arquivo .srt ou .vtt. | Le fichier de sous-titres est illisible. Utilisez un fichier .srt ou .vtt. | Die Untertiteldatei konnte nicht gelesen werden. Verwende eine .srt- oder .vtt-Datei.
Asli | Original | Asal | Original | Original | Original | Original | オリジナル | 원본 | Оригинал
Atas | Top | Atas | Arriba | Topo | Haut | Oben | 上 | 위 | Сверху
Tengah | Middle | Tengah | Centro | Meio | Milieu | Mitte | 中央 | 가운데 | Центр
Bawah | Bottom | Bawah | Abajo | Base | Bas | Unten | 下 | 아래 | Снизу
Kecil | Small | Kecil | Pequeño | Pequeno | Petit | Klein | 小 | 작게 | Мелкий
Sedang | Medium | Sederhana | Mediano | Médio | Moyen | Mittel | 中 | 보통 | Средний
Besar | Large | Besar | Grande | Grande | Grand | Groß | 大 | 크게 | Крупный
Tanpa Animasi | No Animation | Tanpa Animasi | Sin animación | Sem animação | Aucune animation | Keine Animation | アニメーションなし | 애니메이션 없음 | Без анимации
Zoom Denyut | Pulse Zoom | Zum Denyut | Zoom pulsante | Zoom pulsante | Zoom pulsé | Puls-Zoom | パルスズーム | 펄스 줌 | Пульсирующий зум
Zoom Perlahan | Slow Zoom | Zum Perlahan | Zoom lento | Zoom lento | Zoom lent | Langsamer Zoom | スローズーム | 슬로 줌 | Медленный зум
Geser Pelan | Slow Drift | Gelongsor Perlahan | Desplazamiento lento | Deslize lento | Glissement lent | Langsames Gleiten | ゆっくりスライド | 천천히 이동 | Плавный сдвиг
Getar | Shake | Getar | Temblor | Tremor | Secousse | Wackeln | シェイク | 흔들림 | Тряска
Lambat lalu Cepat | Slow then Fast | Perlahan kemudian Laju | Lento y luego rápido | Lento e depois rápido | Lent puis rapide | Langsam dann schnell | スロー→高速 | 느리게→빠르게 | Медленно, затем быстро
Cepat - Lambat - Cepat | Fast - Slow - Fast | Laju - Perlahan - Laju | Rápido - Lento - Rápido | Rápido - Lento - Rápido | Rapide - Lent - Rapide | Schnell - Langsam - Schnell | 高速→スロー→高速 | 빠르게→느리게→빠르게 | Быстро - Медленно - Быстро
Hero | Hero | Hero | Héroe | Herói | Héros | Hero | ヒーロー | 히어로 | Герой
Tanpa Efek | No Effect | Tanpa Kesan | Sin efecto | Sem efeito | Aucun effet | Kein Effekt | エフェクトなし | 효과 없음 | Без эффекта
Cermin | Mirror | Cermin | Espejo | Espelho | Miroir | Spiegel | ミラー | 미러 | Зеркало
Negatif | Negative | Negatif | Negativo | Negativo | Négatif | Negativ | ネガ | 네거티브 | Негатив
Fade Hitam | Fade to Black | Pudar Hitam | Fundido a negro | Fade para preto | Fondu au noir | Ausblenden zu Schwarz | 黒にフェード | 검정 페이드 | Затухание в чёрный
Fade Putih | Fade to White | Pudar Putih | Fundido a blanco | Fade para branco | Fondu au blanc | Ausblenden zu Weiß | 白にフェード | 흰색 페이드 | Затухание в белый
Potong | Cut | Potong | Corte | Corte | Coupe | Schnitt | カット | 컷 | Склейка
Standar | Standard | Standard | Estándar | Padrão | Standard | Standard | 標準 | 기본 | Стандарт
Tebal | Bold | Tebal | Negrita | Negrito | Gras | Fett | 太字 | 굵게 | Жирный
Kuning | Yellow | Kuning | Amarillo | Amarelo | Jaune | Gelb | 黄色 | 노랑 | Жёлтый
Merah | Red | Merah | Rojo | Vermelho | Rouge | Rot | 赤 | 빨강 | Красный
Biru | Blue | Biru | Azul | Azul | Bleu | Blau | 青 | 파랑 | Синий
Hijau | Green | Hijau | Verde | Verde | Vert | Grün | 緑 | 초록 | Зелёный
Besar Tengah | Large Center | Besar Tengah | Grande al centro | Grande no centro | Grand au centre | Groß in der Mitte | 大・中央 | 크게·가운데 | Крупно по центру
Kecil Atas | Small Top | Kecil Atas | Pequeño arriba | Pequeno no topo | Petit en haut | Klein oben | 小・上 | 작게·위 | Мелко сверху
Kecerahan | Brightness | Kecerahan | Brillo | Brilho | Luminosité | Helligkeit | 明るさ | 밝기 | Яркость
Kontras | Contrast | Kontras | Contraste | Contraste | Contraste | Kontrast | コントラスト | 대비 | Контраст
Saturasi | Saturation | Ketepuan | Saturación | Saturação | Saturation | Sättigung | 彩度 | 채도 | Насыщенность
"""

    private const val T3 = """
Eksposur | Exposure | Pendedahan | Exposición | Exposição | Exposition | Belichtung | 露出 | 노출 | Экспозиция
Suhu | Temperature | Suhu | Temperatura | Temperatura | Température | Temperatur | 色温度 | 색온도 | Температура
Rona | Tint | Rona | Matiz | Matiz | Teinte | Farbton | 色合い | 색조 | Оттенок
Sorotan | Highlights | Sorotan | Luces | Realces | Hautes lumières | Lichter | ハイライト | 하이라이트 | Света
Bayangan | Shadows | Bayang | Sombras | Sombras | Ombres | Schatten | シャドウ | 그림자 | Тени
Pudar | Fade | Pudar | Desvanecido | Esmaecido | Atténué | Verblassen | フェード | 페이드 | Выцветание
Unggulan | Featured | Pilihan | Destacados | Destaques | À la une | Empfohlen | おすすめ | 추천 | Избранное
Baru | New | Baharu | Nuevos | Novos | Nouveau | Neu | 新着 | 신규 | Новые
Hit | Popular | Popular | Populares | Populares | Populaires | Beliebt | 人気 | 인기 | Популярные
Kehidupan | Life | Kehidupan | Vida | Vida | Vie | Leben | 日常 | 라이프 | Жизнь
Lanskap | Landscape | Landskap | Paisaje | Paisagem | Paysage | Landschaft | 風景 | 풍경 | Пейзаж
Potret | Portrait | Potret | Retrato | Retrato | Portrait | Porträt | ポートレート | 인물 | Портрет
Mono | Mono | Mono | Mono | Mono | Mono | Mono | モノ | 모노 | Моно
Retro | Retro | Retro | Retro | Retrô | Rétro | Retro | レトロ | 레트로 | Ретро
Senja | Sunset | Senja | Atardecer | Pôr do sol | Crépuscule | Abenddämmerung | 夕暮れ | 노을 | Закат
Pantai | Beach | Pantai | Playa | Praia | Plage | Strand | ビーチ | 해변 | Пляж
Aksi | Action | Aksi | Acción | Ação | Action | Action | アクション | 액션 | Экшен
Awal 2000-an | Early 2000s | Awal 2000-an | Principios de los 2000 | Início dos anos 2000 | Début des années 2000 | Frühe 2000er | 2000年代初頭 | 2000년대 초반 | Начало 2000-х
Kekuatan filter | Filter strength | Kekuatan penapis | Intensidad del filtro | Intensidade do filtro | Intensité du filtre | Filterstärke | フィルターの強さ | 필터 강도 | Сила фильтра
Daftar keterangan | Caption list | Senarai kapsyen | Lista de subtítulos | Lista de legendas | Liste des sous-titres | Untertitelliste | キャプション一覧 | 자막 목록 | Список субтитров
Rotasi saat ini | Current rotation | Putaran semasa | Rotación actual | Rotação atual | Rotation actuelle | Aktuelle Drehung | 現在の回転 | 현재 회전 | Текущий поворот
Terapkan | Apply | Guna | Aplicar | Aplicar | Appliquer | Anwenden | 適用 | 적용 | Применить
ke semua klip | to all clips | pada semua klip | a todos los clips | a todos os clipes | à tous les clips | auf alle Clips | すべてのクリップに | 모든 클립에 | ко всем клипам
Pilih Bahasa | Choose Language | Pilih Bahasa | Elegir idioma | Escolher idioma | Choisir la langue | Sprache wählen | 言語を選択 | 언어 선택 | Выбор языка
Proyek Baru | New Project | Projek Baharu | Proyecto nuevo | Novo projeto | Nouveau projet | Neues Projekt | 新規プロジェクト | 새 프로젝트 | Новый проект
Lihat Semua | See All | Lihat Semua | Ver todo | Ver tudo | Tout voir | Alle ansehen | すべて表示 | 모두 보기 | Все
Menu proyek | Project menu | Menu projek | Menú del proyecto | Menu do projeto | Menu du projet | Projektmenü | プロジェクトメニュー | 프로젝트 메뉴 | Меню проекта
Fitur Premium | Premium Features | Ciri Premium | Funciones premium | Recursos premium | Fonctions premium | Premium-Funktionen | プレミアム機能 | 프리미엄 기능 | Премиум-функции
Filter Warna Premium | Premium Color Filters | Penapis Warna Premium | Filtros de color premium | Filtros de cor premium | Filtres de couleur premium | Premium-Farbfilter | プレミアムカラーフィルター | 프리미엄 컬러 필터 | Премиум-цветофильтры
Transisi Keren | Cool Transitions | Peralihan Hebat | Transiciones geniales | Transições legais | Transitions sympas | Coole Übergänge | かっこいいトランジション | 멋진 전환 | Крутые переходы
Efek Video | Video Effects | Kesan Video | Efectos de vídeo | Efeitos de vídeo | Effets vidéo | Videoeffekte | ビデオエフェクト | 비디오 효과 | Видеоэффекты
Ekspor Tanpa Watermark | Export Without Watermark | Eksport Tanpa Tera Air | Exportar sin marca de agua | Exportar sem marca d'água | Exporter sans filigrane | Export ohne Wasserzeichen | ウォーターマークなしで書き出し | 워터마크 없이 내보내기 | Экспорт без водяного знака
Memuat iklan… | Loading ad… | Memuatkan iklan… | Cargando anuncio… | Carregando anúncio… | Chargement de la pub… | Anzeige wird geladen… | 広告を読み込み中… | 광고 불러오는 중… | Загрузка рекламы…
Iklan siap ditonton. | The ad is ready. | Iklan sedia ditonton. | El anuncio está listo. | O anúncio está pronto. | La pub est prête. | Die Anzeige ist bereit. | 広告の準備ができました。 | 광고가 준비되었습니다. | Реклама готова.
Tonton Iklan untuk Menggunakan Fitur | Watch an Ad to Use Features | Tonton Iklan untuk Guna Ciri | Ver un anuncio para usar las funciones | Assista a um anúncio para usar os recursos | Regarder une pub pour utiliser les fonctions | Anzeige ansehen, um Funktionen zu nutzen | 広告を見て機能を使う | 광고를 보고 기능 사용 | Смотреть рекламу для доступа
Muat Ulang Iklan | Reload Ad | Muat Semula Iklan | Recargar anuncio | Recarregar anúncio | Recharger la pub | Anzeige neu laden | 広告を再読み込み | 광고 다시 불러오기 | Перезагрузить рекламу
Nanti Saja | Not Now | Nanti Sahaja | Ahora no | Agora não | Plus tard | Nicht jetzt | 今はしない | 나중에 | Не сейчас
Tambah video | Add video | Tambah video | Añadir vídeo | Adicionar vídeo | Ajouter une vidéo | Video hinzufügen | 動画を追加 | 동영상 추가 | Добавить видео
Reels & Shorts | Reels & Shorts | Reels & Shorts | Reels y Shorts | Reels e Shorts | Reels et Shorts | Reels & Shorts | リール＆ショート | 릴스 & 쇼츠 | Reels и Shorts
Format vertikal 9:16 untuk Instagram, TikTok, dan YouTube Shorts | Vertical 9:16 format for Instagram, TikTok and YouTube Shorts | Format menegak 9:16 untuk Instagram, TikTok dan YouTube Shorts | Formato vertical 9:16 para Instagram, TikTok y YouTube Shorts | Formato vertical 9:16 para Instagram, TikTok e YouTube Shorts | Format vertical 9:16 pour Instagram, TikTok et YouTube Shorts | Hochformat 9:16 für Instagram, TikTok und YouTube Shorts | Instagram・TikTok・YouTubeショート向けの縦型9:16 | 인스타그램, 틱톡, 유튜브 쇼츠용 세로 9:16 | Вертикальный формат 9:16 для Instagram, TikTok и YouTube Shorts
Feed Persegi | Square Feed | Suapan Segi Empat | Feed cuadrado | Feed quadrado | Feed carré | Quadratischer Feed | 正方形フィード | 정사각형 피드 | Квадратная лента
Rasio 1:1 yang pas untuk feed dan postingan | 1:1 ratio, perfect for feeds and posts | Nisbah 1:1 yang sesuai untuk suapan dan hantaran | Proporción 1:1 ideal para feeds y publicaciones | Proporção 1:1 ideal para feeds e posts | Format 1:1 idéal pour les fils et publications | 1:1-Format, ideal für Feeds und Beiträge | フィードや投稿にぴったりの1:1 | 피드와 게시물에 알맞은 1:1 비율 | Формат 1:1 для ленты и публикаций
Rasio 16:9 untuk YouTube dan layar TV | 16:9 ratio for YouTube and TV screens | Nisbah 16:9 untuk YouTube dan skrin TV | Proporción 16:9 para YouTube y pantallas de TV | Proporção 16:9 para YouTube e telas de TV | Format 16:9 pour YouTube et les écrans TV | 16:9-Format für YouTube und Fernseher | YouTubeやテレビ向けの16:9 | 유튜브와 TV 화면용 16:9 비율 | Формат 16:9 для YouTube и ТВ
Gerak Lambat | Slow Motion | Gerak Perlahan | Cámara lenta | Câmera lenta | Ralenti | Zeitlupe | スローモーション | 슬로 모션 | Замедленная съёмка
Kecepatan 0,5x untuk momen yang dramatis | 0.5x speed for dramatic moments | Kelajuan 0.5x untuk detik yang dramatik | Velocidad 0,5x para momentos dramáticos | Velocidade 0,5x para momentos dramáticos | Vitesse 0,5x pour les moments dramatiques | 0,5-fache Geschwindigkeit für dramatische Momente | ドラマチックな瞬間向けの0.5倍速 | 극적인 순간을 위한 0.5배속 | Скорость 0,5x для драматичных моментов
Timelapse | Timelapse | Selang Masa | Timelapse | Timelapse | Accéléré | Zeitraffer | タイムラプス | 타임랩스 | Таймлапс
Kecepatan 3x untuk perjalanan atau proses panjang | 3x speed for trips or long processes | Kelajuan 3x untuk perjalanan atau proses panjang | Velocidad 3x para viajes o procesos largos | Velocidade 3x para viagens ou processos longos | Vitesse 3x pour les trajets ou les longs processus | 3-fache Geschwindigkeit für Reisen oder lange Abläufe | 旅行や長い工程向けの3倍速 | 여행이나 긴 과정을 위한 3배속 | Скорость 3x для поездок и долгих процессов
Cerah & Segar | Bright & Fresh | Cerah & Segar | Brillante y fresco | Brilhante e fresco | Lumineux et frais | Hell & Frisch | 明るく爽やか | 밝고 상쾌하게 | Ярко и свежо
Filter Cerah agar video lebih hidup | Bright filter to make the video livelier | Penapis Cerah supaya video lebih hidup | Filtro brillante para dar vida al vídeo | Filtro brilhante para deixar o vídeo mais vivo | Filtre lumineux pour un rendu plus vivant | Heller Filter für ein lebendigeres Video | 明るいフィルターで動画を鮮やかに | 밝은 필터로 영상을 생생하게 | Яркий фильтр, чтобы видео стало живее
Montase Cepat | Quick Montage | Montaj Pantas | Montaje rápido | Montagem rápida | Montage rapide | Schnelle Montage | クイックモンタージュ | 빠른 몽타주 | Быстрый монтаж
Pilih beberapa video, tiap video otomatis jadi potongan 1,5 detik. Tinggal ganti video. | Pick several videos; each is cut to 1.5 seconds automatically. Just swap the videos. | Pilih beberapa video, setiap video menjadi potongan 1.5 saat secara automatik. Tinggal tukar video. | Elige varios vídeos; cada uno se recorta a 1,5 segundos automáticamente. Solo cambia los vídeos. | Escolha vários vídeos; cada um é cortado automaticamente em 1,5 segundo. Basta trocar os vídeos. | Choisissez plusieurs vidéos ; chacune est coupée automatiquement à 1,5 seconde. Il suffit de changer les vidéos. | Wähle mehrere Videos; jedes wird automatisch auf 1,5 Sekunden gekürzt. Du tauschst nur die Videos aus. | 複数の動画を選ぶと、それぞれ自動で1.5秒にカットされます。動画を差し替えるだけ。 | 여러 영상을 고르면 각각 자동으로 1.5초로 잘립니다. 영상만 바꾸면 됩니다. | Выберите несколько видео — каждое автоматически обрезается до 1,5 секунды. Остаётся только заменить видео.
Vlog Singkat | Short Vlog | Vlog Ringkas | Vlog corto | Vlog curto | Vlog court | Kurzer Vlog | ショートVlog | 짧은 브이로그 | Короткий влог
"""

    private const val T4 = """
Tiap video otomatis jadi potongan 3 detik dalam format vertikal. | Each video is automatically cut to 3 seconds in vertical format. | Setiap video menjadi potongan 3 saat secara automatik dalam format menegak. | Cada vídeo se recorta automáticamente a 3 segundos en formato vertical. | Cada vídeo é cortado automaticamente em 3 segundos no formato vertical. | Chaque vidéo est coupée automatiquement à 3 secondes en format vertical. | Jedes Video wird automatisch auf 3 Sekunden im Hochformat gekürzt. | 各動画は縦型で自動的に3秒にカットされます。 | 각 영상이 세로 형식으로 자동으로 3초로 잘립니다. | Каждое видео автоматически обрезается до 3 секунд в вертикальном формате.
Tambah Teks | Add Text | Tambah Teks | Añadir texto | Adicionar texto | Ajouter du texte | Text hinzufügen | テキストを追加 | 텍스트 추가 | Добавить текст
Maksimal 5 teks | Maximum 5 texts | Maksimum 5 teks | Máximo 5 textos | Máximo 5 textos | 5 textes maximum | Maximal 5 Texte | 最大5件 | 최대 5개 | Максимум 5 текстов
Hapus teks | Delete text | Padam teks | Eliminar texto | Excluir texto | Supprimer le texte | Text löschen | テキストを削除 | 텍스트 삭제 | Удалить текст
Hapus keterangan | Delete caption | Padam kapsyen | Eliminar subtítulo | Excluir legenda | Supprimer le sous-titre | Untertitel löschen | キャプションを削除 | 자막 삭제 | Удалить субтитр
Ekstrak audio | Extract audio | Ekstrak audio | Extraer audio | Extrair áudio | Extraire l'audio | Audio extrahieren | 音声を抽出 | 오디오 추출 | Извлечь аудио
Ketuk klip atau audio untuk memilih. Geser untuk menggulung, cubit untuk zoom. | Tap a clip or audio to select it. Swipe to scroll, pinch to zoom. | Ketik klip atau audio untuk memilih. Seret untuk menatal, cubit untuk zum. | Toca un clip o audio para seleccionarlo. Desliza para desplazarte, pellizca para hacer zoom. | Toque em um clipe ou áudio para selecionar. Deslize para rolar, pince para dar zoom. | Touchez un clip ou un audio pour le sélectionner. Faites glisser pour défiler, pincez pour zoomer. | Tippe auf einen Clip oder Audio zum Auswählen. Wischen zum Scrollen, zwei Finger zum Zoomen. | クリップや音声をタップして選択。スワイプでスクロール、ピンチでズーム。 | 클립이나 오디오를 탭해 선택하세요. 밀어서 스크롤, 핀치로 확대/축소합니다. | Нажмите на клип или аудио, чтобы выбрать. Проведите для прокрутки, щипком масштабируйте.
Tarik handle putih di ujung klip untuk memotong. | Drag the white handle at the clip's end to trim. | Seret pemegang putih di hujung klip untuk memotong. | Arrastra el control blanco en el extremo del clip para recortar. | Arraste a alça branca na ponta do clipe para cortar. | Faites glisser la poignée blanche au bord du clip pour couper. | Ziehe den weißen Griff am Clip-Ende, um zu kürzen. | クリップ端の白いハンドルをドラッグしてトリム。 | 클립 끝의 흰색 핸들을 끌어 자르세요. | Потяните белый маркер на краю клипа, чтобы обрезать.
Geser audio untuk memindah. Tarik handle putih untuk memotong. | Drag the audio to move it. Drag the white handle to trim. | Seret audio untuk memindah. Seret pemegang putih untuk memotong. | Arrastra el audio para moverlo. Arrastra el control blanco para recortar. | Arraste o áudio para mover. Arraste a alça branca para cortar. | Faites glisser l'audio pour le déplacer. Faites glisser la poignée blanche pour couper. | Ziehe das Audio zum Verschieben. Ziehe den weißen Griff zum Kürzen. | 音声をドラッグして移動。白いハンドルをドラッグしてトリム。 | 오디오를 끌어 이동하세요. 흰색 핸들을 끌어 자르세요. | Перетащите аудио, чтобы переместить. Потяните белый маркер, чтобы обрезать.
Export gagal | Export failed | Eksport gagal | Error al exportar | Falha na exportação | Échec de l'export | Export fehlgeschlagen | 書き出しに失敗しました | 내보내기 실패 | Ошибка экспорта
Coba Lagi | Try Again | Cuba Lagi | Reintentar | Tentar novamente | Réessayer | Erneut versuchen | もう一度試す | 다시 시도 | Повторить
Kembali ke Editor | Back to Editor | Kembali ke Editor | Volver al editor | Voltar ao editor | Retour à l'éditeur | Zurück zum Editor | エディターに戻る | 편집기로 돌아가기 | Назад в редактор
Mulai Export | Start Export | Mula Eksport | Iniciar exportación | Iniciar exportação | Lancer l'export | Export starten | 書き出しを開始 | 내보내기 시작 | Начать экспорт
Berhasil! | Success! | Berjaya! | ¡Listo! | Pronto! | Réussi ! | Geschafft! | 完了！ | 완료! | Готово!
Video telah disimpan di galeri Anda | The video was saved to your gallery | Video telah disimpan dalam galeri anda | El vídeo se guardó en tu galería | O vídeo foi salvo na sua galeria | La vidéo a été enregistrée dans votre galerie | Das Video wurde in deiner Galerie gespeichert | 動画をギャラリーに保存しました | 영상이 갤러리에 저장되었습니다 | Видео сохранено в галерее
Buka Galeri | Open Gallery | Buka Galeri | Abrir galería | Abrir galeria | Ouvrir la galerie | Galerie öffnen | ギャラリーを開く | 갤러리 열기 | Открыть галерею
dtk | sec | saat | s | s | s | Sek. | 秒 | 초 | с
Berlaku untuk klip | Applies to clip | Terpakai untuk klip | Se aplica al clip | Aplica-se ao clipe | S'applique au clip | Gilt für Clip | 適用先クリップ | 적용 대상 클립 | Применяется к клипу
Suara ikut menyesuaikan tanpa mengubah nada. | Sound follows without changing pitch. | Bunyi turut menyesuaikan tanpa mengubah nada. | El sonido se ajusta sin cambiar el tono. | O som acompanha sem alterar o tom. | Le son suit sans changer la hauteur. | Der Ton passt sich ohne Tonhöhenänderung an. | 音程は変えずに音声も合わせて変わります。 | 음높이는 그대로 소리도 함께 조절됩니다. | Звук подстраивается без изменения тона.
Volume Klip | Clip Volume | Kelantangan Klip | Volumen del clip | Volume do clipe | Volume du clip | Clip-Lautstärke | クリップの音量 | 클립 볼륨 | Громкость клипа
Perkiraan ukuran file | Estimated file size | Anggaran saiz fail | Tamaño estimado del archivo | Tamanho estimado do arquivo | Taille estimée du fichier | Geschätzte Dateigröße | 推定ファイルサイズ | 예상 파일 크기 | Примерный размер файла
Durasi | Duration | Tempoh | Duración | Duração | Durée | Dauer | 長さ | 길이 | Длительность
klip | clips | klip | clips | clipes | clips | Clips | クリップ | 클립 | клипов
Premium aktif • sekitar | Premium active • about | Premium aktif • lebih kurang | Premium activo • aprox. | Premium ativo • cerca de | Premium actif • environ | Premium aktiv • etwa | プレミアム有効・約 | 프리미엄 활성 • 약 | Премиум активен • около
menit lagi | minutes left | minit lagi | min restantes | min restantes | min restantes | Min. übrig | 分 | 분 남음 | мин осталось
Maksimal | Maximum | Maksimum | Máximo | Máximo | Maximum | Maximal | 最大 | 최대 | Максимум
klip audio | audio clips | klip audio | clips de audio | clipes de áudio | clips audio | Audio-Clips | 音声クリップ | 오디오 클립 | аудиоклипов
keterangan | captions | kapsyen | subtítulos | legendas | sous-titres | Untertitel | キャプション | 자막 | субтитров
keterangan diimpor. | captions imported. | kapsyen diimport. | subtítulos importados. | legendas importadas. | sous-titres importés. | Untertitel importiert. | 件のキャプションを読み込みました。 | 개의 자막을 가져왔습니다. | субтитров импортировано.
Klip terlalu pendek untuk speed ramp (minimal 3 detik). | The clip is too short for a speed ramp (at least 3 seconds). | Klip terlalu pendek untuk speed ramp (minimum 3 saat). | El clip es demasiado corto para una rampa de velocidad (mínimo 3 segundos). | O clipe é curto demais para uma rampa de velocidade (mínimo 3 segundos). | Le clip est trop court pour une rampe de vitesse (3 secondes minimum). | Der Clip ist zu kurz für eine Geschwindigkeitsrampe (mindestens 3 Sekunden). | クリップが短すぎてスピードランプにできません(3秒以上必要)。 | 클립이 너무 짧아 스피드 램프를 쓸 수 없습니다(최소 3초). | Клип слишком короткий для рампы скорости (минимум 3 секунды).
teks, stiker, atau overlay | texts, stickers, or overlays | teks, pelekat atau tindanan | textos, pegatinas o superposiciones | textos, figurinhas ou overlays | textes, stickers ou incrustations | Texte, Sticker oder Overlays | テキスト・ステッカー・オーバーレイ | 텍스트, 스티커 또는 오버레이 | текстов, стикеров или оверлеев
Jelas | Clear | Jelas | Nítido | Nítido | Net | Klar | クリア | 선명 | Четкий
Segar | Fresh | Segar | Fresco | Fresco | Frais | Frisch | フレッシュ | 상쾌 | Свежий
Rapi | Neat | Rapi | Pulcro | Limpo | Soigné | Gepflegt | すっきり | 깔끔 | Опрятный
Hangat Lembut | Soft Warm | Hangat Lembut | Cálido suave | Quente suave | Doux chaud | Weich warm | やわらか暖色 | 부드러운 따뜻함 | Мягкое тепло
Kafe | Café | Kafe | Café | Café | Café | Café | カフェ | 카페 | Кафе
Harian | Everyday | Harian | Diario | Diário | Quotidien | Alltag | 日常 | 일상 | Будни
Bersih | Clean | Bersih | Limpio | Limpo | Propre | Sauber | クリーン | 깨끗함 | Чистый
Hutan | Forest | Hutan | Bosque | Floresta | Forêt | Wald | 森 | 숲 | Лес
Gunung | Mountain | Gunung | Montaña | Montanha | Montagne | Berg | 山 | 산 | Горы
Langit Biru | Blue Sky | Langit Biru | Cielo azul | Céu azul | Ciel bleu | Blauer Himmel | 青空 | 푸른 하늘 | Синее небо
Kabut | Fog | Kabut | Niebla | Névoa | Brouillard | Nebel | 霧 | 안개 | Туман
Kota Neon | Neon City | Kota Neon | Ciudad neón | Cidade neon | Ville néon | Neon-Stadt | ネオンシティ | 네온 시티 | Неоновый город
Danau | Lake | Danau | Lago | Lago | Lac | See | 湖 | 호수 | Озеро
Musim Gugur | Autumn | Musim Gugur | Otoño | Outono | Automne | Herbst | 秋 | 가을 | Осень
Jernih HD | Crystal HD | Jernih HD | HD cristalino | HD cristalino | HD cristallin | Kristall-HD | クリアHD | 선명 HD | Чистый HD
Porselen | Porcelain | Porselen | Porcelana | Porcelana | Porcelaine | Porzellan | 磁器 | 도자기 | Фарфор
Bersinar | Glowing | Bersinar | Resplandor | Brilhante | Éclatant | Strahlend | 輝き | 빛나는 | Сияние
Fokus | Focus | Fokus | Enfoque | Foco | Mise au point | Fokus | フォーカス | 포커스 | Фокус
Matte | Matte | Matte | Mate | Fosco | Mat | Matt | マット | 매트 | Матовый
Remaja | Youthful | Remaja | Juvenil | Juvenil | Jeune | Jugendlich | ヤング | 청춘 | Молодость
Logam | Metal | Logam | Metal | Metal | Métal | Metall | メタル | 메탈 | Металл
Peach | Peach | Peach | Melocotón | Pêssego | Pêche | Pfirsich | ピーチ | 피치 | Персик
Senyap | Quiet | Senyap | Silencio | Silêncio | Calme | Leise | 静か | 고요 | Тишина
"""

    private const val T5 = """
Hitam Putih | Black & White | Hitam Putih | Blanco y negro | Preto e branco | Noir et blanc | Schwarzweiß | 白黒 | 흑백 | Чёрно-белый
HP Kontras | B&W Contrast | HP Kontras | B/N contraste | P&B contraste | N&B contraste | SW Kontrast | 白黒コントラスト | 흑백 대비 | Ч/б контраст
HP Lembut | B&W Soft | HP Lembut | B/N suave | P&B suave | N&B doux | SW weich | 白黒ソフト | 흑백 부드럽게 | Ч/б мягкий
Noir | Noir | Noir | Noir | Noir | Noir | Noir | ノワール | 누아르 | Нуар
Matte Noir | Matte Noir | Matte Noir | Noir mate | Noir fosco | Noir mat | Mattes Noir | マットノワール | 매트 누아르 | Матовый нуар
Perak | Silver | Perak | Plata | Prata | Argent | Silber | シルバー | 실버 | Серебро
Kertas Koran | Newsprint | Kertas Koran | Papel prensa | Papel jornal | Papier journal | Zeitungspapier | 新聞紙 | 신문지 | Газетная бумага
Mono Hangat | Warm Mono | Mono Hangat | Mono cálido | Mono quente | Mono chaud | Warmes Mono | 暖色モノ | 따뜻한 모노 | Тёплое моно
Film Dingin | Cool Film | Film Dingin | Cine frío | Filme frio | Film froid | Kühler Film | クールフィルム | 차가운 필름 | Холодная плёнка
Film Hangat | Warm Film | Film Hangat | Cine cálido | Filme quente | Film chaud | Warmer Film | ウォームフィルム | 따뜻한 필름 | Тёплая плёнка
Sinema | Cinema | Sinema | Cine | Cinema | Cinéma | Kino | シネマ | 시네마 | Кино
Warna Analog | Analog Color | Warna Analog | Color analógico | Cor analógica | Couleur analogique | Analoge Farben | アナログ色 | 아날로그 색감 | Аналоговый цвет
Hijau Analog | Analog Green | Hijau Analog | Verde analógico | Verde analógico | Vert analogique | Analoges Grün | アナロググリーン | 아날로그 그린 | Аналоговый зелёный
Pucat Sinema | Bleach Bypass | Pucat Sinema | Bleach bypass | Bleach bypass | Bleach bypass | Bleach Bypass | ブリーチバイパス | 블리치 바이패스 | Отбеливание
Drama | Drama | Drama | Drama | Drama | Drame | Drama | ドラマ | 드라마 | Драма
Sutradara | Director | Sutradara | Director | Diretor | Réalisateur | Regisseur | ディレクター | 감독 | Режиссёр
Retro 70an | Retro 70s | Retro 70an | Retro 70s | Retrô anos 70 | Rétro années 70 | Retro 70er | レトロ70s | 70년대 레트로 | Ретро 70-х
Kaset VHS | VHS Tape | Kaset VHS | Cinta VHS | Fita VHS | Cassette VHS | VHS-Kassette | VHSテープ | VHS 테이프 | Кассета VHS
Polaroid | Polaroid | Polaroid | Polaroid | Polaroid | Polaroid | Polaroid | ポラロイド | 폴라로이드 | Полароид
Sepia | Sepia | Sepia | Sepia | Sépia | Sépia | Sepia | セピア | 세피아 | Сепия
Foto Tua | Old Photo | Foto Tua | Foto antigua | Foto antiga | Vieille photo | Altes Foto | 古い写真 | 오래된 사진 | Старое фото
Memudar | Faded | Memudar | Desvaído | Desbotado | Délavé | Verblasst | 色あせ | 빛바램 | Выцветший
Cetak Lawas | Old Print | Cetak Lawas | Impresión antigua | Impressão antiga | Vieille impression | Alter Druck | 古い印刷 | 옛날 인쇄 | Старая печать
Jam Emas | Golden Hour | Jam Emas | Hora dorada | Hora dourada | Heure dorée | Goldene Stunde | ゴールデンアワー | 골든 아워 | Золотой час
Senja Ungu | Purple Dusk | Senja Ungu | Crepúsculo morado | Crepúsculo roxo | Crépuscule violet | Violette Dämmerung | 紫の夕暮れ | 보라빛 노을 | Лиловый закат
Matahari Terbit | Sunrise | Matahari Terbit | Amanecer | Nascer do sol | Lever du soleil | Sonnenaufgang | 日の出 | 일출 | Восход
Romansa | Romance | Romansa | Romance | Romance | Romance | Romantik | ロマンス | 로맨스 | Романтика
Langit Api | Fire Sky | Langit Api | Cielo de fuego | Céu de fogo | Ciel de feu | Feuerhimmel | 炎の空 | 불타는 하늘 | Огненное небо
Laut | Ocean | Laut | Océano | Oceano | Océan | Ozean | 海 | 바다 | Океан
Pulau Biru | Blue Island | Pulau Biru | Isla azul | Ilha azul | Île bleue | Blaue Insel | ブルーアイランド | 블루 아일랜드 | Синий остров
Liburan | Vacation | Liburan | Vacaciones | Férias | Vacances | Urlaub | バカンス | 휴가 | Отпуск
Tropis | Tropical | Tropis | Tropical | Tropical | Tropical | Tropisch | トロピカル | 트로피칼 | Тропики
Pasir | Sand | Pasir | Arena | Areia | Sable | Sand | 砂 | 모래 | Песок
Karang | Coral | Karang | Coral | Coral | Corail | Koralle | サンゴ | 산호 | Коралл
Ombak | Waves | Ombak | Olas | Ondas | Vagues | Wellen | 波 | 파도 | Волны
Gelap Tegas | Bold Dark | Gelap Tegas | Oscuro intenso | Escuro forte | Sombre intense | Kräftig dunkel | 力強いダーク | 강렬한 어두움 | Резкий тёмный
Baja | Steel | Baja | Acero | Aço | Acier | Stahl | スチール | 스틸 | Сталь
Panas | Hot | Panas | Caliente | Quente | Chaud | Heiß | ホット | 핫 | Жаркий
Tegang | Tense | Tegang | Tenso | Tenso | Tendu | Angespannt | 緊張 | 긴장 | Напряжение
Tajam | Sharp | Tajam | Nítido | Nítido | Précis | Scharf | シャープ | 샤프 | Резкий
Y2K Biru | Y2K Blue | Y2K Biru | Y2K azul | Y2K azul | Y2K bleu | Y2K Blau | Y2Kブルー | Y2K 블루 | Y2K синий
Kamera Digital | Digital Camera | Kamera Digital | Cámara digital | Câmera digital | Appareil photo numérique | Digitalkamera | デジカメ | 디지털 카메라 | Цифровая камера
Lampu Kilat | Flash | Lampu Kilat | Flash | Flash | Flash | Blitz | フラッシュ | 플래시 | Вспышка
Pop | Pop | Pop | Pop | Pop | Pop | Pop | ポップ | 팝 | Поп
Piringan | Disc | Piringan | Disco | Disco | Disque | Scheibe | ディスク | 디스크 | Диск
Kampus | Campus | Kampus | Campus | Campus | Campus | Campus | キャンパス | 캠퍼스 | Кампус
Pagi Cerah | Bright Morning | Pagi Cerah | Mañana radiante | Manhã radiante | Matin lumineux | Heller Morgen | 明るい朝 | 화창한 아침 | Яркое утро
Hari Biasa | Ordinary Day | Hari Biasa | Día normal | Dia comum | Jour ordinaire | Gewöhnlicher Tag | 普通の日 | 평범한 날 | Обычный день
Rumah | Home | Rumah | Hogar | Lar | Maison | Zuhause | 家 | 집 | Дом
Sahabat | Friends | Sahabat | Amigos | Amigos | Amis | Freunde | 友達 | 친구 | Друзья
Jalan-jalan | Outing | Jalan-jalan | Paseo | Passeio | Balade | Ausflug | お出かけ | 나들이 | Прогулка
Makan Enak | Tasty Food | Makan Enak | Comida rica | Comida gostosa | Bonne cuisine | Leckeres Essen | おいしい食事 | 맛있는 음식 | Вкусная еда
Kota Hidup | Lively City | Kota Hidup | Ciudad viva | Cidade viva | Ville animée | Lebendige Stadt | にぎやかな街 | 활기찬 도시 | Живой город
Santai | Relaxed | Santai | Relajado | Relaxado | Détendu | Entspannt | のんびり | 여유 | Расслабленный
Ceria | Cheerful | Ceria | Alegre | Alegre | Joyeux | Fröhlich | 陽気 | 명랑 | Весёлый
Natural | Natural | Natural | Natural | Natural | Naturel | Natürlich | ナチュラル | 내추럴 | Натуральный
Sawah | Rice Field | Sawah | Arrozal | Arrozal | Rizière | Reisfeld | 田んぼ | 논 | Рисовое поле
Bukit | Hills | Bukit | Colinas | Colinas | Collines | Hügel | 丘 | 언덕 | Холмы
Awan | Clouds | Awan | Nubes | Nuvens | Nuages | Wolken | 雲 | 구름 | Облака
Embun | Dew | Embun | Rocío | Orvalho | Rosée | Tau | 朝露 | 이슬 | Роса
Padang Hijau | Green Meadow | Padang Hijau | Prado verde | Campo verde | Prairie verte | Grüne Wiese | 緑の草原 | 푸른 초원 | Зелёный луг
Air Terjun | Waterfall | Air Terjun | Cascada | Cachoeira | Cascade | Wasserfall | 滝 | 폭포 | Водопад
Salju | Snow | Salju | Nieve | Neve | Neige | Schnee | 雪 | 눈 | Снег
Gurun | Desert | Gurun | Desierto | Deserto | Désert | Wüste | 砂漠 | 사막 | Пустыня
Rimba | Jungle | Rimba | Selva | Selva | Jungle | Dschungel | ジャングル | 정글 | Джунгли
Cakrawala | Horizon | Cakrawala | Horizonte | Horizonte | Horizon | Horizont | 地平線 | 지평선 | Горизонт
Lembut | Soft | Lembut | Suave | Suave | Doux | Weich | ソフト | 부드러움 | Мягкий
Kulit Halus | Smooth Skin | Kulit Halus | Piel suave | Pele suave | Peau lisse | Glatte Haut | なめらか肌 | 매끈한 피부 | Гладкая кожа
Wajah Cerah | Bright Face | Wajah Cerah | Rostro luminoso | Rosto iluminado | Visage lumineux | Strahlendes Gesicht | 明るい顔色 | 화사한 얼굴 | Яркое лицо
Mata Tajam | Sharp Eyes | Mata Tajam | Ojos nítidos | Olhos nítidos | Regard perçant | Scharfe Augen | くっきり目元 | 또렷한 눈 | Чёткий взгляд
Studio | Studio | Studio | Estudio | Estúdio | Studio | Studio | スタジオ | 스튜디오 | Студия
Elegan | Elegant | Elegan | Elegante | Elegante | Élégant | Elegant | エレガント | 우아함 | Элегантный
Manis | Sweet | Manis | Dulce | Doce | Doux | Süß | スイート | 달콤함 | Милый
Klasik | Classic | Klasik | Clásico | Clássico | Classique | Klassisch | クラシック | 클래식 | Классика
Segar Muda | Youthful Fresh | Segar Muda | Fresco juvenil | Fresco jovem | Frais et jeune | Jugendfrisch | 若々しい | 싱그러움 | Юная свежесть
Hangat Manis | Sweet Warm | Hangat Manis | Cálido dulce | Quente doce | Chaud doux | Süß warm | 甘い暖色 | 달콤 따뜻함 | Сладкое тепло
Abu Lembut | Soft Gray | Abu Lembut | Gris suave | Cinza suave | Gris doux | Weiches Grau | やわらかグレー | 부드러운 회색 | Мягкий серый
Grafit | Graphite | Grafit | Grafito | Grafite | Graphite | Graphit | グラファイト | 그래파이트 | Графит
Arang | Charcoal | Arang | Carbón | Carvão | Charbon | Kohle | チャコール | 차콜 | Уголь
Tinta | Ink | Tinta | Tinta | Tinta | Encre | Tinte | インク | 잉크 | Чернила
Kabut Abu | Gray Mist | Kabut Abu | Niebla gris | Névoa cinza | Brume grise | Grauer Dunst | グレーの霞 | 회색 안개 | Серая дымка
Putih Tinggi | High Key | Putih Tinggi | Clave alta | Tom alto | Haute clé | High Key | ハイキー | 하이키 | Высокий ключ
Hitam Dalam | Deep Black | Hitam Dalam | Negro profundo | Preto profundo | Noir profond | Tiefschwarz | 深い黒 | 깊은 검정 | Глубокий чёрный
Dokumenter | Documentary | Dokumenter | Documental | Documentário | Documentaire | Dokumentarisch | ドキュメンタリー | 다큐멘터리 | Документальный
Jalanan | Street | Jalanan | Calle | Rua | Rue | Straße | ストリート | 거리 | Улица
Seni | Art | Seni | Arte | Arte | Art | Kunst | アート | 아트 | Искусство
Malam Kota | City Night | Malam Kota | Noche urbana | Noite urbana | Nuit urbaine | Stadtnacht | 街の夜 | 도시의 밤 | Ночной город
Layar Lebar | Widescreen | Layar Lebar | Pantalla ancha | Tela larga | Grand écran | Breitbild | ワイドスクリーン | 와이드스크린 | Широкий экран
Analog 35 | Analog 35 | Analog 35 | Analógico 35 | Analógico 35 | Analogique 35 | Analog 35 | アナログ35 | 아날로그 35 | Аналог 35
Teater | Theater | Teater | Teatro | Teatro | Théâtre | Theater | シアター | 극장 | Театр
Detektif | Detective | Detektif | Detective | Detetive | Détective | Detektiv | 探偵 | 탐정 | Детектив
Romantis Film | Film Romance | Romantis Film | Romance de cine | Romance de cinema | Romance filmée | Filmromantik | 映画ロマンス | 영화 로맨스 | Киноромантика
Epik | Epic | Epik | Épico | Épico | Épique | Episch | エピック | 에픽 | Эпик
"""

    private const val T6 = """
Indie | Indie | Indie | Indie | Indie | Indé | Indie | インディー | 인디 | Инди
Thriller | Thriller | Thriller | Thriller | Thriller | Thriller | Thriller | スリラー | 스릴러 | Триллер
Klasik Hollywood | Classic Hollywood | Klasik Hollywood | Hollywood clásico | Hollywood clássico | Hollywood classique | Klassisches Hollywood | クラシックハリウッド | 클래식 할리우드 | Классический Голливуд
Kaset Lama | Old Cassette | Kaset Lama | Casete antiguo | Fita antiga | Vieille cassette | Alte Kassette | 古いカセット | 오래된 카세트 | Старая кассета
Disko | Disco | Disko | Disco | Discoteca | Disco | Disco | ディスコ | 디스코 | Диско
Album Keluarga | Family Album | Album Keluarga | Álbum familiar | Álbum de família | Album de famille | Familienalbum | 家族アルバム | 가족 앨범 | Семейный альбом
Kartu Pos | Postcard | Kartu Pos | Postal | Cartão-postal | Carte postale | Postkarte | ポストカード | 엽서 | Открытка
Arcade | Arcade | Arcade | Arcade | Arcade | Arcade | Arcade | アーケード | 아케이드 | Аркада
Vintage Kuning | Yellow Vintage | Vintage Kuning | Vintage amarillo | Vintage amarelo | Vintage jaune | Gelber Vintage | 黄色ビンテージ | 노랑 빈티지 | Жёлтый винтаж
Layar Tabung | CRT Screen | Layar Tabung | Pantalla CRT | Tela CRT | Écran CRT | Röhrenbildschirm | ブラウン管 | 브라운관 | Экран ЭЛТ
Jadul | Old School | Jadul | De antaño | Antigão | Old school | Old School | 懐かしい | 옛날식 | Старая школа
Majalah Lama | Old Magazine | Majalah Lama | Revista antigua | Revista antiga | Vieux magazine | Altes Magazin | 古い雑誌 | 오래된 잡지 | Старый журнал
Radio Tua | Old Radio | Radio Tua | Radio antigua | Rádio antigo | Vieille radio | Altes Radio | 古いラジオ | 오래된 라디오 | Старое радио
Merah Jambu | Pink | Merah Jambu | Rosa | Rosa | Rose | Rosa | ピンク | 핑크 | Розовый
Jingga Pekat | Deep Orange | Jingga Pekat | Naranja intenso | Laranja intenso | Orange profond | Tieforange | 濃いオレンジ | 진한 오렌지 | Насыщенный оранжевый
Langit Magenta | Magenta Sky | Langit Magenta | Cielo magenta | Céu magenta | Ciel magenta | Magenta-Himmel | マゼンタの空 | 마젠타 하늘 | Пурпурное небо
Sore Emas | Golden Afternoon | Sore Emas | Tarde dorada | Tarde dourada | Après-midi doré | Goldener Nachmittag | 黄金の午後 | 황금빛 오후 | Золотой вечер
Remang | Twilight | Remang | Penumbra | Penumbra | Pénombre | Dämmerlicht | 薄明かり | 어스름 | Полумрак
Fajar | Dawn | Fajar | Alba | Amanhecer | Aube | Morgendämmerung | 夜明け | 새벽 | Рассвет
Cahaya Hangat | Warm Light | Cahaya Hangat | Luz cálida | Luz quente | Lumière chaude | Warmes Licht | 暖かい光 | 따뜻한 빛 | Тёплый свет
Bara | Embers | Bara | Brasas | Brasas | Braises | Glut | 燃えさし | 불씨 | Угли
Lembayung | Afterglow | Lembayung | Arrebol | Arrebol | Lueur du soir | Abendrot | 夕焼け | 저녁놀 | Закатное зарево
Pelangi Sore | Evening Rainbow | Pelangi Sore | Arcoíris vespertino | Arco-íris da tarde | Arc-en-ciel du soir | Abendregenbogen | 夕方の虹 | 저녁 무지개 | Вечерняя радуга
Biru Tosca | Turquoise | Biru Tosca | Turquesa | Turquesa | Turquoise | Türkis | ターコイズ | 터코이즈 | Бирюзовый
Laguna | Lagoon | Laguna | Laguna | Lagoa | Lagon | Lagune | ラグーン | 라군 | Лагуна
Pasir Putih | White Sand | Pasir Putih | Arena blanca | Areia branca | Sable blanc | Weißer Sand | 白い砂 | 하얀 모래 | Белый песок
Selancar | Surf | Selancar | Surf | Surfe | Surf | Surfen | サーフ | 서핑 | Серфинг
Pulau Kecil | Small Island | Pulau Kecil | Isla pequeña | Ilha pequena | Petite île | Kleine Insel | 小さな島 | 작은 섬 | Малый остров
Teluk | Bay | Teluk | Bahía | Baía | Baie | Bucht | 湾 | 만 | Залив
Siang Laut | Sea Noon | Siang Laut | Mediodía marino | Meio-dia no mar | Midi marin | Meeresmittag | 海の正午 | 바다의 한낮 | Морской полдень
Karang Cerah | Bright Coral | Karang Cerah | Coral brillante | Coral brilhante | Corail vif | Helle Koralle | 明るいサンゴ | 밝은 산호 | Яркий коралл
Kapal Layar | Sailboat | Kapal Layar | Velero | Veleiro | Voilier | Segelboot | ヨット | 요트 | Парусник
Camar | Seagull | Camar | Gaviota | Gaivota | Mouette | Möwe | カモメ | 갈매기 | Чайка
Kejar | Chase | Kejar | Persecución | Perseguição | Poursuite | Verfolgung | 追跡 | 추격 | Погоня
Ledakan | Explosion | Ledakan | Explosión | Explosão | Explosion | Explosion | 爆発 | 폭발 | Взрыв
Balap | Racing | Balap | Carreras | Corrida | Course | Rennen | レース | 레이싱 | Гонки
Medan Laga | Battlefield | Medan Laga | Campo de batalla | Campo de batalha | Champ de bataille | Schlachtfeld | 戦場 | 전장 | Поле боя
Olahraga | Sports | Olahraga | Deportes | Esportes | Sport | Sport | スポーツ | 스포츠 | Спорт
Pertarungan | Fight | Pertarungan | Pelea | Luta | Combat | Kampf | 格闘 | 대결 | Бой
Adrenalin | Adrenaline | Adrenalin | Adrenalina | Adrenalina | Adrénaline | Adrenalin | アドレナリン | 아드레날린 | Адреналин
Garang | Fierce | Garang | Feroz | Feroz | Féroce | Wild | 猛々しい | 사나움 | Свирепый
Baja Dingin | Cold Steel | Baja Dingin | Acero frío | Aço frio | Acier froid | Kalter Stahl | 冷たい鋼 | 차가운 강철 | Холодная сталь
Sirkuit | Circuit | Sirkuit | Circuito | Circuito | Circuit | Rennstrecke | サーキット | 서킷 | Трасса
Handycam | Handycam | Handycam | Handycam | Handycam | Handycam | Handycam | ハンディカム | 핸디캠 | Хэндикам
MTV | MTV | MTV | MTV | MTV | MTV | MTV | MTV | MTV | MTV
Warnet | Internet Café | Warnet | Cibercafé | Lan house | Cybercafé | Internetcafé | ネットカフェ | PC방 | Интернет-кафе
Ponsel Lipat | Flip Phone | Ponsel Lipat | Móvil plegable | Celular flip | Téléphone à clapet | Klapphandy | ガラケー | 폴더폰 | Раскладушка
Foto Cetak | Photo Print | Foto Cetak | Foto impresa | Foto impressa | Tirage photo | Fotoabzug | プリント写真 | 인화 사진 | Фотопечать
Kamera Saku | Pocket Camera | Kamera Saku | Cámara compacta | Câmera compacta | Compact photo | Kompaktkamera | コンパクトカメラ | 포켓 카메라 | Компакт-камера
Pesta Sekolah | School Party | Pesta Sekolah | Fiesta escolar | Festa escolar | Fête scolaire | Schulparty | 学園パーティー | 학교 파티 | Школьная вечеринка
Boyband | Boy Band | Boyband | Boy band | Boy band | Boys band | Boyband | ボーイバンド | 보이밴드 | Бойз-бэнд
Lensa Murah | Cheap Lens | Lensa Murah | Lente barata | Lente barata | Objectif bon marché | Billiglinse | 安価レンズ | 저가 렌즈 | Дешёвый объектив
Cerah Murah | Cheap Bright | Cerah Murah | Brillo barato | Brilho barato | Lumineux bon marché | Billig-Hell | 安っぽい明るさ | 저렴한 밝음 | Дешёвая яркость
Viral | Viral | Viral | Viral | Viral | Viral | Viral | バイラル | 바이럴 | Вирусный
Populer | Popular | Populer | Popular | Popular | Populaire | Beliebt | 人気 | 인기 | Популярный
Estetik | Aesthetic | Estetik | Estético | Estético | Esthétique | Ästhetisch | エステティック | 감성 | Эстетика
Favorit | Favorite | Favorit | Favorito | Favorito | Favori | Favorit | お気に入り | 즐겨찾기 | Любимый
Terlaris | Best Seller | Terlaris | Más vendido | Mais vendido | Meilleure vente | Bestseller | ベストセラー | 베스트셀러 | Бестселлер
Kekinian | Trendy | Kekinian | De moda | Da moda | Tendance | Trendig | トレンド | 트렌디 | Модный
Mantap | Awesome | Mantap | Genial | Incrível | Génial | Großartig | すごい | 멋짐 | Классный
Juara | Champion | Juara | Campeón | Campeão | Champion | Champion | チャンピオン | 챔피언 | Чемпион
Top Harian | Daily Top | Top Harian | Top diario | Top diário | Top du jour | Tages-Top | 今日のトップ | 오늘의 인기 | Топ дня
Pilihan Editor | Editor's Pick | Pilihan Editor | Selección del editor | Escolha do editor | Choix de l'éditeur | Tipp der Redaktion | 編集部のおすすめ | 에디터 추천 | Выбор редактора
Cerah | Bright | Cerah | Brillante | Brilhante | Lumineux | Hell | 明るい | 밝게 | Яркий
"""

    private val TABLE: String = T0 + T1 + T2 + T3 + T4 + T5 + T6

}
fun tr(s: String): String = L10n.tr(s)
