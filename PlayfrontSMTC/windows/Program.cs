using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.Media.Control;
using Windows.Storage.Streams;

namespace PlayfrontSMTC
{
    class Program
    {
        static async Task Main(string[] args)
        {
            Console.OutputEncoding = Encoding.UTF8;

            try
            {
                var manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync();

                string action = args.Length > 0 ? args[0].ToLower() : "info";
                string targetAppId = args.Length > 1 ? args[1] : "default";

                // --- 1. HANDLE "CYCLE" PLAYER ACTION ---
                if (action == "cycle")
                {
                    var sessionsList = manager.GetSessions().OrderBy(s => s.SourceAppUserModelId).ToList();

                    if (sessionsList.Count == 0) return;

                    int currentIndex = -1;
                    if (targetAppId != "default")
                    {
                        currentIndex = sessionsList.FindIndex(s => s.SourceAppUserModelId == targetAppId);
                    }

                    // Mathematical loop to the next app (safely resets to 0 if at the end)
                    int nextIndex = (currentIndex + 1) % sessionsList.Count;

                    Console.WriteLine(sessionsList[nextIndex].SourceAppUserModelId);
                    return;
                }

                // --- 2. FIND SPECIFIED SESSION ---
                GlobalSystemMediaTransportControlsSession session = null;
                if (targetAppId != "default")
                {
                    session = manager.GetSessions().FirstOrDefault(s => s.SourceAppUserModelId == targetAppId);
                }

                // Fallback to Windows default session if not found or closed
                if (session == null)
                {
                    session = manager.GetCurrentSession();
                }

                if (session == null) return;

                string activeAppId = session.SourceAppUserModelId;

                // --- 3. HANDLE MEDIA CONTROL COMMANDS ---
                switch (action)
                {
                    case "play": await session.TryPlayAsync(); return;
                    case "pause": await session.TryPauseAsync(); return;
                    case "toggle": await session.TryTogglePlayPauseAsync(); return;
                    case "next": await session.TrySkipNextAsync(); return;
                    case "prev": await session.TrySkipPreviousAsync(); return;
                    case "info": break; // Proceed to metadata polling
                    default: return;
                }

                // --- 4. READ & OUTPUT METADATA ---
                var mediaProps = await session.TryGetMediaPropertiesAsync();
                var timeline = session.GetTimelineProperties();
                var playbackInfo = session.GetPlaybackInfo();

                string title = mediaProps.Title ?? "";
                string artist = mediaProps.Artist ?? "";

                double positionSeconds = timeline.Position.TotalSeconds;
                bool isPlaying = playbackInfo.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing;

                if (isPlaying)
                {
                    double timePassed = (DateTimeOffset.Now - timeline.LastUpdatedTime).TotalSeconds;

                    // Removed the < 15 limit! Trust the OS math no matter how long it's been in the background.
                    if (timePassed >= 0)
                    {
                        double rate = playbackInfo.PlaybackRate ?? 1.0;
                        positionSeconds += timePassed * rate;
                    }
                }

                double durationSeconds = timeline.EndTime.TotalSeconds;

                // Safety catch: Don't let the position exceed the total duration
                if (positionSeconds > durationSeconds && durationSeconds > 0)
                {
                    positionSeconds = durationSeconds;
                }

                double progress = durationSeconds > 0 ? Math.Clamp(positionSeconds / durationSeconds, 0.0, 1.0) : 0.0;

                string artPath = Path.Combine(Path.GetTempPath(), "playfront_art.jpg");
                string cachePath = Path.Combine(Path.GetTempPath(), "playfront_last_song.txt");
                string currentSongKey = $"{activeAppId}|{title}|{artist}";

                bool songChanged = true;
                try
                {
                    if (File.Exists(cachePath))
                    {
                        if (File.ReadAllText(cachePath) == currentSongKey && File.Exists(artPath))
                            songChanged = false;
                    }
                }
                catch { }

                if (songChanged && mediaProps.Thumbnail != null)
                {
                    try
                    {
                        using var stream = await mediaProps.Thumbnail.OpenReadAsync();
                        using var reader = new DataReader(stream);
                        await reader.LoadAsync((uint)stream.Size);

                        byte[] buffer = new byte[stream.Size];
                        reader.ReadBytes(buffer);
                        File.WriteAllBytes(artPath, buffer);
                        File.WriteAllText(cachePath, currentSongKey);
                    }
                    catch { }
                }
                else if (!File.Exists(artPath))
                {
                    artPath = "";
                }

                Console.WriteLine($"{title}\t{artist}\t{progress}\t{artPath}\t{positionSeconds}\t{durationSeconds}\t{isPlaying}\t{activeAppId}");
            }
            catch (Exception)
            {
                // Silently fail
            }
        }
    }
}