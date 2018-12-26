const context = {
  tmtAuth: {
    hasRealmRole: () => {return true},
    hasResourceRole: () => {return true}
  },
  isAuthenticated: true
};

export const TMTAuthContext = ({
  Consumer(props) {
    return props.children(context)
  }
});
