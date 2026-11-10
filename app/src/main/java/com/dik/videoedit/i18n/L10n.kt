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
    private const val TABLE = """
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
Cerah | Bright | Cerah | Brillante | Brilhante | Lumineux | Hell | 明るい | 밝게 | Яркий
"""
}
fun tr(s: String): String = L10n.tr(s)
