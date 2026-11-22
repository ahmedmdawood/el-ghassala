# Room opens AppDatabase_Impl by name. Preserve this small persistence package,
# including its generated implementation, entities and converters.
-keep class dev.ahmeddawood.elghassala.data.db.** { *; }

# Keep metadata used by library reflection. Compose, Lifecycle, Room and
# ProfileInstaller supply their own consumer rules through their AARs.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Backup/settings JSON use explicit string keys (no reflection-based serializer).
# Avatar file names and joke asset paths are data, not obfuscated class names.
# Do not blanket-keep Compose: that would prevent useful R8 optimization.
