# Overridden by command-line arguments in jpackage
Summary: alchemist
Name: alchemist
Version: 0.1.0
Release: 1
License: Unknown
Vendor: Unknown

%if "xhttps://alchemistsimulator.github.io/" != "x"
URL: https://alchemistsimulator.github.io/
%endif

%if "x/opt" != "x"
Prefix: /opt
%endif

Provides: alchemist

%if "x" != "x"
Group: 
%endif

Autoprov: 0
Autoreq: 0
%if "xxdg-utils" != "x" || "x" != "x"
Requires: xdg-utils 
%endif

%define __jar_repack %{nil}

%define package_filelist %{_tmppath}/%{name}.files
%define app_filelist %{_tmppath}/%{name}.app.files
%define filesystem_filelist %{_tmppath}/%{name}.filesystem.files

%define default_filesystem / /opt /usr /usr/bin /usr/lib /usr/local /usr/local/bin /usr/local/lib

%description
alchemist

%global __os_install_post %{nil}

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}/usr/lib/alchemist
cp -r %{_sourcedir}/opt/alchemist/* %{buildroot}/usr/lib/alchemist
ln -s "/usr/lib/alchemist/bin/alchemist" "%{buildroot}/usr/bin/alchemist"
%if "x/home/zimbrando/Projects/Alchemist/LICENSE.md" != "x"
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:/home/zimbrando/Projects/Alchemist/LICENSE.md}
  install -d -m 755 "%{buildroot}%{dirname:%{license_install_file}}"
  install -m 644 "/home/zimbrando/Projects/Alchemist/LICENSE.md" "%{buildroot}%{license_install_file}"
%endif
(cd %{buildroot} && find . -type d) | sed -e 's/^\.//' -e '/^$/d' | sort > %{app_filelist}
{ rpm -ql filesystem || echo %{default_filesystem}; } | sort > %{filesystem_filelist}
comm -23 %{app_filelist} %{filesystem_filelist} > %{package_filelist}
sed -i -e 's/.*/%dir "&"/' %{package_filelist}
(cd %{buildroot} && find . -not -type d) | sed -e 's/^\.//' -e 's/.*/"&"/' >> %{package_filelist}
%if "x/home/zimbrando/Projects/Alchemist/LICENSE.md" != "x"
  sed -i -e 's|"%{license_install_file}"||' -e '/^$/d' %{package_filelist}
%endif

%files -f %{package_filelist}
%if "x/home/zimbrando/Projects/Alchemist/LICENSE.md" != "x"
  %license "%{license_install_file}"
%endif

%post
xdg-desktop-menu install /opt/alchemist/lib/alchemist-alchemist.desktop

%preun

xdg-desktop-menu uninstall /opt/alchemist/lib/alchemist-alchemist.desktop

%clean