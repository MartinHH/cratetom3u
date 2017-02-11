# CrateToM3U

A simple command-line tool to convert `.crate` files (created by
[Serato](https://serato.com/) software like Serato DJ, Scratch Live or
Itch) to `.m3u` playlist files.

(Note that currently, "smart crates" are not supported.)


## Usage

Enter `cratetom3u --help` for a list of all options:

```
MyMachine:~ myuser$ cratetom3u --help
CrateToM3U 0.1.0
CrateToM3U is a tool to convert Serato .crate files to .m3u playlist files.
(Please note that "smart crates" are not supported.)

Options:

  -i, --input  <arg>     path to input .crate file (required)
  -o, --output  <arg>    path to output .m3u file (required)

  -a, --add  <arg>       audio file paths substring to prepend
  -c, --charset  <arg>   charset for the output file (default is UTF-16)
  -r, --remove  <arg>    audio file paths substring to remove (supports regex)
      --help             Show help message
      --version          Show version of this program
```

So a basic example would be this:

```
yMachine:~ myuser$ cratetom3u -i ~/Music/_Serato_/Crates/Example.crate -o ~/Example.m3u
[CrateToM3U]: Wrote 50 tracks to /Users/MyUser/Example.m3u
```

However, since the audio file paths in `.crate` files are stored without
a leading `/` in front of the root directory, you might need to prepend
the missing `/` using the `-a` option:

```
yMachine:~ myuser$ cratetom3u -i ~/Music/_Serato_/Crates/Example.crate -o ~/Example.m3u -a /
[CrateToM3U]: Wrote 50 tracks to /Users/MyUser/Example.m3u
```

On a Mac with Serato installed to its default location, the above
command should create a working `m3u` file that can be used with VLC,
Itunes etc.